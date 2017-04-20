package forward_enumeration.enum_abstract.components;

import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.AggrEnumerator;
import global.GlobalConfig;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsRenameNode;
import lang.sql.ast.abstable.AbsSelectNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.NamedVal;
import lang.sql.ast.val.ValNode;
import lang.sql.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerate aggregation nodes with provided enum context
 * Created by clwang on 1/7/16.
 */
public class EnumAggrTableNode {

    // When this flag is true, we will not allow comparison between multiple aggregation fields
    public static final boolean SIMPLIFY = GlobalConfig.SIMPLIFY_AGGR_FIELD;

    /***********************************************************
     * Enum by Aggregation
     *  1. Enumerate the group-by fields
     *      1) should be able to do some real aggregation
     *  2. Enumerate the target field
     *  3. Enumerate the aggregation function
     *      2) based on the type of the target field
     ***********************************************************/

    public static List<AbsTableNode> enumAggrNodeWFilter(EnumContext ec) {

        boolean simplify = SIMPLIFY;
        boolean withFilter = true;

        List<AbsTableNode> coreTableNodes = ec.getTableNodes();

        List<AbsTableNode> aggregationNodes = new ArrayList<AbsTableNode>();
        for (AbsTableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(generalEnumAggrPerTable(ec, coreTable, simplify, Optional.ofNullable(null), withFilter));
        }

        return aggregationNodes;
    }

    // the following two are functions for emit enumerating the tables.
    public static List<Table> emitEnumAggrNodeWFilter(EnumContext ec, MemQueryContainer qc) {

        Set<Table> newlyGeneratedTables = new HashSet<>();

        List<AbsTableNode> coreTableNodes = ec.getTableNodes();
        for (AbsTableNode coreTable : coreTableNodes) {
            List<AbsRenameNode> aggrNodes = AggrEnumerator.enumerateAggregation(ec, coreTable, SIMPLIFY);
            for (AbsRenameNode rt : aggrNodes) {

                // filters for aggregation fields are listed here
                List<AbsTableNode> result = new ArrayList<>();

                List<ValNode> vals = rt.getSchema().stream()
                        .map(s -> new NamedVal(s))
                        .collect(Collectors.toList());
                AbsTableNode filtered = RenameTNWrapper.tryRename(new AbsSelectNode(vals, rt));
                    result.add(filtered);

                for (AbsTableNode tn : result) {
                    try {
                        Table resultT = tn.eval(new Environment());
                        Table originalT = coreTable.eval(new Environment());

                        if (originalT.getContent().isEmpty() || resultT.getContent().isEmpty())
                            continue;

                        qc.insertQuery(tn);

                        // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(originalT),
                                qc.getRepresentative(resultT));

                        newlyGeneratedTables.add(qc.getRepresentative(resultT));

                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return newlyGeneratedTables.stream().collect(Collectors.toList());
    }

    /**
     * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode if optionalQC is not provided
     */
    private static List<AbsTableNode> generalEnumAggrPerTable(
            EnumContext ec,
            AbsTableNode tn,
            boolean simplify,
            Optional<MemQueryContainer> optionalQC,
            boolean withFilter) {

        List<AbsTableNode> result = new ArrayList<>();
        List<AbsRenameNode> aggrNodes = AggrEnumerator.enumerateAggregation(ec, tn, simplify);

        if (withFilter) {

            for (AbsRenameNode rt : aggrNodes) {
                // filters for aggregation fields are listed here
                List<ValNode> vals = rt.getSchema().stream()
                        .map(NamedVal::new)
                        .collect(Collectors.toList());
                AbsTableNode filtered = RenameTNWrapper.tryRename(new AbsSelectNode(vals, rt));

                result.add(filtered);
            }
        } else {
            result = aggrNodes.stream().map(x -> x).collect(Collectors.toList());
        }

        // a possible way to speedup this process is to make this emission process into the enumeration process
        if (optionalQC.isPresent()) {
            for (AbsTableNode x : result) {
                emitToQueryChest(x, tn, optionalQC.get());
            }
        }

        return result;
    }

    private static void emitToQueryChest(AbsTableNode result, AbsTableNode original, MemQueryContainer qc) {

        try {
            Table resultT = result.eval(new Environment());
            Table originalT = original.eval(new Environment());

            if (originalT.getContent().isEmpty() || resultT.getContent().isEmpty())
                return;

            qc.insertQuery(result);

            // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
            qc.getTableLinks().insertEdge(
                    qc.getRepresentative(originalT),
                    qc.getRepresentative(resultT));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

}
