package forward_enumeration.enum_abstract;

import backward_inference.MappingInference;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import forward_enumeration.AbstractTableEnumerator;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.container.SimpleQueryContainer;
import forward_enumeration.enum_abstract.components.*;
import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import global.GlobalConfig;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsProjNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.table.Table;
import lang.table.TableAttr;
import sun.java2d.pipe.SpanShapeRenderer;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class AbsQueryEnumerator extends AbstractTableEnumerator {

    @Override
    public List<AbsTableNode> enumTable(EnumContext ec, int depth) {

        QueryContainer qc = new SimpleQueryContainer();
        // qc memoizes these intermediate results, since the result pool is shared
        enumJoinWithoutProj(ec, qc, depth);

        System.out.println(qc.printStatus());

        return qc.getCollectedQueries();
    }

    public static QueryContainer simpleEnumTableWithoutProj(EnumContext ec, QueryContainer qc, int depth) {

        ec.setTableNodes(qc.getCollectedQueries());
        List<AbsTableNode> tns = EnumSelect.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.collectQueries(tns, x -> true);

        ec.setTableNodes(qc.getCollectedQueries());
        tns = EnumAggrTableNode.enumAggrNodeWFilter(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.collectQueries(tns, x -> true);

        for (int i = 1; i <= depth; i ++) {
            ec.setTableNodes(qc.getCollectedQueries());
            tns = EnumJoinTableNodes.enumJoinWithFilter(ec);
            System.out.println("There are " + tns.size() + " tables in the enumeration of this level(" + i + ")");
            qc.collectQueries(tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()),
                              x -> true);
        }

        return qc;
    }

    private static void tryCollect(QueryContainer qc, List<AbsTableNode> tns, EnumContext ec) {
        qc.collectQueries(tns.stream().map(tn -> new AbsProjNode(tn)).collect(Collectors.toList()), tn -> {
            if (GlobalConfig.firstPhasePruningTech.equals("Constraint"))
                return checkValidityWithProp(tn, ec.getOutputTable(), ec.getInputs());
            else if (GlobalConfig.firstPhasePruningTech.equals("Approximation"))
                return checkValidityWithApproximation(tn, ec.getOutputTable());
            else return false;
        });
    }

    private static boolean checkValidityWithProp(AbsTableNode atn, Table output, List<Table> inputTables) {
        Context ctx = new Context();
        Solver solver = ctx.mkSolver();
        Map<AbsTableNode, TableAttr> map = new HashMap<>();
        atn.genConstraints(ctx, solver, map);

        Set<Value> newValues = MappingInference.inverseTable(output).keySet();
        for (Table in : inputTables)
            newValues.removeAll(MappingInference.inverseTable(in).keySet());

        solver.add(ctx.mkEq(map.get(atn).r, ctx.mkInt(output.getContent().size())));
        solver.add(ctx.mkEq(map.get(atn).c, ctx.mkInt(output.getSchema().size())));
        solver.add(ctx.mkEq(map.get(atn).newVal, ctx.mkInt(newValues.size())));

        if (solver.check() == Status.SATISFIABLE)
            return true;
        else
            return false;
    }

    private static boolean checkValidityWithApproximation(AbsTableNode atn, Table output) {
        try {
            MappingInference mi = MappingInference.buildMapping(atn.eval(new Environment()), output);
            if (! mi.everyCellHasImage())
                return false;
            return true;
        } catch (SQLEvalException e) {}
        return false;
    }

    public static QueryContainer enumJoinWithoutProj(EnumContext ec, QueryContainer qc, int depth) {

        //##### Synthesize SPJ query
        ec.setTableNodes(ec.getInputs().stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList()));
        List<AbsTableNode> basic = EnumSelect.enumFilterNamed(ec);
        tryCollect(qc, basic, ec);

        System.out.println("[Stage 1] EnumFilterNamed: \n\t"
                + "Total Table by now: " + qc.getCollectedQueries().size());
        if (qc instanceof MemQueryContainer) {
            System.out.println("\tAvg table size: " + (((MemQueryContainer) qc).getMemoizedTables()
                .stream().map(t -> t.getContent().size() * t.getSchema().size())
                .reduce((x,y)-> x + y).get() / ((MemQueryContainer) qc).getMemoizedTables().size()));
        }

        //##### Synthesize AGGR
        ec.setTableNodes(basic);
        List<AbsTableNode> aggr = EnumAggrTableNode.enumAggrNodeWFilter(ec);
        tryCollect(qc, aggr, ec);

        List<AbsTableNode> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(basic);
        basicAndAggr.addAll(aggr);

        System.out.println("[Stage 2] EnumAggregationNode: \n\t"
                + "Total Table by now: "
                + qc.getCollectedQueries().size());
        if (qc instanceof MemQueryContainer) {
            System.out.println("Avg table size: " + (((MemQueryContainer) qc).getMemoizedTables()
                    .stream().map(t -> t.getContent().size() * t.getSchema().size())
                    .reduce((x,y)-> x + y).get() / ((MemQueryContainer) qc).getMemoizedTables().size()));
        }

        if (depth == 0) return qc;

        //##### Synthesize Join
        List<AbsTableNode> leftSet = basicAndAggr;
        for (int i = 1; i <= depth - 1; i ++) {

            // TODO: add some break point

            leftSet = EnumJoinTableNodes.enumJoinLeftRight(leftSet, basicAndAggr, ec);
            tryCollect(qc, leftSet, ec);

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAndBasic" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total table by now: "
                    + qc.getCollectedQueries().size());
            if (qc instanceof MemQueryContainer) {
                System.out.println("Avg table size: " + (((MemQueryContainer) qc).getMemoizedTables()
                        .stream().map(t -> t.getContent().size() * t.getSchema().size())
                        .reduce((x,y)-> x + y).get() / ((MemQueryContainer) qc).getMemoizedTables().size()));
            }
        }

        // TODO: add some break point

        //##### Synthesize Union
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection, return if so
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0)
            // return;

            leftSet = EnumUnion.enumUnion(leftSet, basic);
            tryCollect(qc, leftSet, ec);

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumLeftJoin" + i + " \n\t"
                    + "Total Table by now: " + qc.getCollectedQueries().size());
        }

        // TODO: add some break point

        //##### Synthesize LeftJoin
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0)
            // return;

            leftSet = EnumLeftJoin.enumLeftJoin(leftSet, basic);

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumUnion" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getCollectedQueries().size());
        }

        // TODO: add some break point

        //##### Synthesize Aggregation on Join

        if (GlobalConfig.testComplexAggregation != -1) {
            leftSet = basic;
            for (int i = 1; i <= GlobalConfig.testComplexAggregation; i ++) {

                // TODO: add some break point

                leftSet = EnumJoinTableNodes.enumJoinLeftRight(leftSet, basic, ec);

                //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
                System.out.println("[Stage " + (2 + i) + "] EnumAggrOnJoin" + i + " \n\t"
                        // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                        + "Total table by now: " + qc.getCollectedQueries().size());
            }
            if (leftSet != basic) {
                ec.setTableNodes(leftSet);
                List<AbsTableNode> tmp = EnumAggrTableNode.enumAggrNodeWFilter(ec);
                tryCollect(qc, tmp, ec);
            }

            //##### Synthesize join with aggregation result
            leftSet = basicAndAggr;
            for (int i = 1; i <= GlobalConfig.testComplexAggregation; i ++) {

                // TODO: add some break point

                leftSet = EnumJoinTableNodes.enumJoinLeftRight(leftSet, basicAndAggr, ec);
                tryCollect(qc, leftSet, ec);

                //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
                System.out.println("[Stage " + (2 + i) + "] EnumJoinTwoAggr" + i + " \n\t"
                        // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                        + "Total Table by now: "
                        + qc.getCollectedQueries().size());
            }
        }
        // TODO: add some break point

        //##### Synthesize Aggr after Aggr
        ec.setTableNodes(aggr);
        List<AbsTableNode> aggrAfterAggr = EnumAggrTableNode.enumAggrNodeWFilter(ec);
        leftSet = aggrAfterAggr;
        tryCollect(qc, leftSet, ec);
        for (int i = 1; i <= depth-1; i ++) {
            // check whether a result can be obtained by projection
            //if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0)
            // return;

            leftSet = EnumJoinTableNodes.enumJoinLeftRight(leftSet, basic, ec);
            tryCollect(qc, leftSet, ec);
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAggr" + i + " \n\t"
                    + "Total Table by now: " + qc.getCollectedQueries().size());
            if (qc instanceof MemQueryContainer) {
                System.out.println("Avg table size: " + (((MemQueryContainer) qc).getMemoizedTables()
                    .stream().map(t -> t.getContent().size() * t.getSchema().size())
                    .reduce((x,y)-> x + y).get() / ((MemQueryContainer) qc).getMemoizedTables().size()));
            }
        }

        return qc;
    }

}
