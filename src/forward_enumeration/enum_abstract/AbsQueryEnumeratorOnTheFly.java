package forward_enumeration.enum_abstract;

import forward_enumeration.AbstractTableEnumerator;
import forward_enumeration.enum_abstract.components.*;
import forward_enumeration.enum_abstract.datastructure.TableTreeNode;
import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import global.GlobalConfig;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsJoinNode;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.exception.SQLEvalException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/31/16.
 * Perform enumeration based on SynthSQL grammar, and enumeration results are stored into query chests on the fly.
 */
public class AbsQueryEnumeratorOnTheFly extends AbstractTableEnumerator {

    @Override
    public List<AbsTableNode> enumTable(EnumContext ec, int depth) {

        List<AbsTableNode> result = new ArrayList<>();

        MemQueryContainer qc = MemQueryContainer.initWithInputTables(ec.getInputs(), MemQueryContainer.ContainerType.TableLinks);

        enumTableWithoutProj(ec, qc, depth); // all intermediate result are stored in qc

        System.out.println("[Total Number of Intermediate Result] " + qc.getRepresentativeTableNodes().size() );
        double tableSizeSum = 0;
        for (AbsTableNode tn : qc.getRepresentativeTableNodes()) {
            tableSizeSum += ((AbsNamedTable)tn).getTable().getContent().size()
                    *  ((AbsNamedTable)tn).getTable().getContent().get(0).getValues().size();
        }
        System.out.println("[Average Size of the tables] " + tableSizeSum / (qc.getRepresentativeTableNodes().size()));
        System.out.println("[Sum Size of the tables] " + tableSizeSum);

        EnumProjection.emitEnumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable(), qc);

        // this is not always enabled, as this export algorithm is only designed for canonical SQL enumerator
        if (qc.getContainerType() == MemQueryContainer.ContainerType.TableLinks) {

            Set<Table> leafNodes = new HashSet<>();
            leafNodes.addAll(ec.getInputs());
            List<TableTreeNode> trees = qc.getTableLinks().findTableTrees(ec.getOutputTable(), leafNodes, 5);

            int totalQueryCount = 0;
            for (TableTreeNode t : trees) {
                t.inferQuery(ec);
                result.addAll(t.treeToQuery());

                int count = t.countQueryNum();
                //System.out.println("Queries corresponds to this tree: " + count);
                totalQueryCount += count;
            }

            System.out.println("Total Tree Count: " + trees.size());
            System.out.println("Total Query Count: " + totalQueryCount);
        }

        return result;
    }

    public static void enumTableWithoutProj(EnumContext ec, MemQueryContainer qc, int depth) {

        //##### Synthesize SPJ query
        ec.setTableNodes(ec.getInputs().stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList()));
        List<AbsTableNode> basic = EnumSelect.emitEnumFilterNamed(ec, qc)
                .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

        System.out.println("[Per Stage Reduction Rate] " + ((double)qc.getRepresentativeTableNodes().size()) / ec.getInputs().size());

        System.out.println("[Stage 1] EnumFilterNamed: \n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());

        //##### Synthesize AGGR
        ec.setTableNodes(basic);
        List<AbsTableNode> aggr = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc)
                .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

        List<AbsTableNode> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(basic);
        basicAndAggr.addAll(aggr);

        System.out.println("[Stage 2] EnumAggregationNode: \n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());

        if (depth == 0) return;

        //##### Synthesize Join
        List<AbsTableNode> leftSet = basicAndAggr;
        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            // check whether a result can be obtained by projection
            //List<TableNode> tns = EnumProjection.enumProjection(leftSet, ec.getOutputTable());
            //if (i >= 2 && ! tns.isEmpty())
             //   break;

            List<AbsTableNode> oldLeft = leftSet;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAndBasic" + i + " \n\t"
                   // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total table by now: " + qc.getMemoizedTables().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());

            if (GlobalConfig.STAT_MODE) {
                Set<Table> tables = new HashSet<>();
                for (AbsTableNode tn : leftSet) {
                    try {
                        tables.add(tn.eval(new Environment()));
                    } catch (SQLEvalException e) {}
                }
                Set<Table> t2 = new HashSet<>();
                for (AbsTableNode tn1 : oldLeft) {
                    for (AbsTableNode tn2 : basic) {
                        try {
                            t2.add(new AbsJoinNode(Arrays.asList(tn1, tn2)).eval(new Environment()));
                        } catch (SQLEvalException e) {
                        }
                    }
                }

                System.out.println("[JPer Stage Reduction Rate] " + ((double) tables.size()) / (t2.size()));
            }

            //System.out.println("[Join Per Stage Reduction Rate] " + ((double)qc.getRepresentativeTableNodes().size()) / ec.getInputs().size());

        }
        if (! leftSet.equals(basicAndAggr))
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;

        //##### Synthesize Union
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection, return if so
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumUnion.emitEnumerateUnion(leftSet, basic, qc)
                    .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumLeftJoin" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        }

        if (!leftSet.equals(basic)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize LeftJoin
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumLeftJoin.emitEnumLeftJoin(leftSet, basic, qc)
                        .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumUnion" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        }

        if (! leftSet.equals(basic)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize Aggregation on Join
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {
            // check whether a result can be obtained by projection
            //if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;
            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAndBasic" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables()
                    .stream()
                    .map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
        if (leftSet != basic) {
            ec.setTableNodes(leftSet);
            List<AbsTableNode> tmp = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc).stream()
                    .map(t -> new AbsNamedTable(t)).collect(Collectors.toList());
            if (EnumProjection.enumProjection(tmp, ec.getOutputTable()).size() > 0) return;
        }


        //##### Synthesize join with aggregation result
        leftSet = basicAndAggr;
        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            // check whether a result can be obtained by projection
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basicAndAggr, ec, qc)
                    .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinTwoAggr" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
        if (!leftSet.equals(basicAndAggr)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize Aggr after Aggr
        ec.setTableNodes(aggr);
        List<AbsTableNode> aggrAfterAggr = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc)
                .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());
        leftSet = aggrAfterAggr;
        for (int i = 1; i <= depth-1; i ++) {
            // check whether a result can be obtained by projection
            //if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList());
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAggr" + i + " \n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + qc.getMemoizedTables().stream()
                    .map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
    }
}
