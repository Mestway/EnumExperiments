package forward_enumeration.enum_abstract.components;

import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsJoinNode;
import lang.sql.ast.abstable.AbsRenameNode;
import lang.sql.ast.abstable.AbsSelectNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.NamedVal;
import lang.sql.ast.val.ValNode;
import lang.sql.exception.SQLEvalException;
import util.CombinationGenerator;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Enumerate join table nodes with given EC
 * Created by clwang on 1/7/16.
 */
public class EnumJoinTableNodes {

    public static List<AbsTableNode> enumJoinLeftRight(List<AbsTableNode> left, List<AbsTableNode> right, EnumContext ec) {

        List<AbsTableNode> result = new ArrayList<>();

        for (AbsTableNode ti : left) {
            for (AbsTableNode tj : right) {

                if (ti.getTableName() == tj.getTableName()) {
                    tj = RenameTNWrapper.tryRename(tj);
                }

                List<AbsTableNode> tns = Arrays.asList(ti, tj);
                AbsJoinNode jn = new AbsJoinNode(tns);
                AbsRenameNode rt = (AbsRenameNode) RenameTNWrapper.tryRename(jn);

                result.add(rt);

                // the selection args are complete
                List<ValNode> vals = rt.getSchema().stream()
                        .map(s -> new NamedVal(s))
                        .collect(Collectors.toList());

                AbsTableNode resultTn = new AbsSelectNode(vals, rt);

                try {
                    Table tns0 = tns.get(0).eval(new Environment());
                    Table tns1 = tns.get(1).eval(new Environment());
                    Table resultT = resultTn.eval(new Environment());

                    if (tns0.getContent().isEmpty() || tns1.getContent().isEmpty() || resultT.isEmpty())
                        continue;

                    result.add(resultTn);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    /**
     * The general emit join node function, other emit function are supposed to be implemented around this one.
     * @param ec the enumeration context
     * @param qc the query chest for emission
     */
    public static List<Table> generalEmitEnumJoin(
            List<AbsTableNode> leftSet,
            List<AbsTableNode> rightSet,
            EnumContext ec,
            MemQueryContainer qc) {

        List<Table> newlyGeneratedTable = new ArrayList<>();
        
        for (AbsTableNode ti : leftSet) {
            for (AbsTableNode tj : rightSet) {

                List<AbsTableNode> tns = Arrays.asList(ti, tj);

                AbsJoinNode jn = new AbsJoinNode(tns);

                AbsRenameNode rt = (AbsRenameNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                qc.insertQuery(rt);
                try {
                    qc.getTableLinks().insertEdge(
                            qc.getRepresentative(tns.get(0).eval(new Environment())),
                            qc.getRepresentative(tns.get(1).eval(new Environment())),
                            qc.getRepresentative(rt.eval(new Environment())));
                    newlyGeneratedTable.add(qc.getRepresentative(qc.getRepresentative(rt.eval(new Environment()))));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                // the selection args are complete
                List<ValNode> vals = rt.getSchema().stream()
                        .map(s -> new NamedVal(s))
                        .collect(Collectors.toList());
                AbsTableNode resultTn = new AbsSelectNode(vals, rt);

                try {
                    Table tns0 = tns.get(0).eval(new Environment());
                    Table tns1 = tns.get(1).eval(new Environment());
                    Table resultT = resultTn.eval(new Environment());

                    if (tns0.getContent().isEmpty() || tns1.getContent().isEmpty() || resultT.isEmpty()) {
                        continue;
                    }

                    qc.insertQuery(RenameTNWrapper.tryRename(resultTn));

                    qc.getTableLinks().insertEdge(
                            qc.getRepresentative(tns0),
                            qc.getRepresentative(tns1),
                            qc.getRepresentative(resultT));

                    newlyGeneratedTable.add(qc.getRepresentative(resultT));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return newlyGeneratedTable;
    }

    //Similar to general emit enum join, except that tables are not emitted on the fly
    public static List<AbsTableNode> generalEnumJoin(
            int tableNum,
            EnumContext ec,
            BiFunction<EnumContext, List<AbsTableNode>, Boolean> checker,
            boolean withFilter) {

        List<AbsTableNode> result = new ArrayList<>();

        List<AbsTableNode> basicTables = ec.getTableNodes();

        // table combinations
        List<List<AbsTableNode>> tableComb = CombinationGenerator.genMultPermutation(basicTables, tableNum);

        for (List<AbsTableNode> tns : tableComb) {
            if (!checker.apply(ec, tns))
                continue;
            AbsJoinNode jn = new AbsJoinNode(tns);

            if (withFilter == false) {
                result.add(jn);
            }else {
                AbsRenameNode rt = (AbsRenameNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                result.add(rt);

                // the selection args are complete
                List<ValNode> vals = rt.getSchema().stream()
                        .map(s -> new NamedVal(s))
                        .collect(Collectors.toList());
                result.add(RenameTNWrapper.tryRename(new AbsSelectNode(vals, rt)));
            }
        }

        return result;
    }

    /*****************************************************
     Enumeration by join
     1. Enumerate atomic tables and then do join
     *****************************************************/

    // This is a simpler version of joining considering no filters at this stage,
    // Joining is only a matter of performing cartesian production here.
    public static List<AbsJoinNode> enumJoinWithoutFilter(EnumContext ec) {
        List<AbsTableNode> tns = generalEnumJoin(2, ec, atMostOneNoneInput, false);
        return tns.stream().map(tn -> (AbsJoinNode) tn).collect(Collectors.toList());
    }

    // This is the join we used in canonicalSQL,
    // filters are used in enumerating canonical join nodes.
    public static List<AbsTableNode> enumJoinWithFilter(EnumContext ec) {
        return generalEnumJoin(2, ec, atMostOneNoneInput, true);
    }

    public static final BiFunction<EnumContext, List<AbsTableNode>, Boolean> atMostOneNoneInput = (ec, lst) -> {
        int noneInputNodeCnt = lst.stream().map(tn -> ec.isInputTableNode(tn) ? 0 : 1).reduce(0, (x,y) -> x + y);
        if (noneInputNodeCnt > 1)
            return false;
        return true;
    };

}
