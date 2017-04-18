package forward_enumeration.enum_abstract.components;

import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.LeftJoinEnumerator;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsLeftJoinNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 10/22/16.
 */
public class EnumLeftJoin {


    public static List<AbsLeftJoinNode> enumLeftJoin(EnumContext ec) {
        return LeftJoinEnumerator.enumLeftJoinFromEC(ec);
    }

    public static List<AbsTableNode> enumLeftJoin(
            List<AbsTableNode> leftSet,
            List<AbsTableNode> rightSet) {

        List<AbsTableNode> newlyGeneratedTable = new ArrayList<>();

        for (int i = 0; i < leftSet.size(); i ++) {
            for (int j = 0; j < rightSet.size(); j ++) {
                List<AbsLeftJoinNode> ljns = LeftJoinEnumerator.enumLeftJoin(leftSet.get(i), rightSet.get(j));
                newlyGeneratedTable.addAll(ljns);
            }
        }

        return newlyGeneratedTable;
    }

    public static List<Table> emitEnumLeftJoin(
            List<AbsTableNode> leftSet,
            List<AbsTableNode> rightSet,
            MemQueryContainer qc) {

        List<Table> newlyGeneratedTable = new ArrayList<>();

        for (int i = 0; i < leftSet.size(); i ++) {
            for (int j = 0; j < rightSet.size(); j ++) {
                List<AbsLeftJoinNode> ljns = LeftJoinEnumerator.enumLeftJoin(leftSet.get(i), rightSet.get(j));

                for (AbsTableNode ljn : ljns) {
                    try {
                        Table ljnt = ljn.eval(new Environment());
                        if (qc != null) {
                            qc.insertQuery(ljn);
                            ljnt = qc.getRepresentative(ljnt);
                            qc.getTableLinks().insertEdge(
                                    qc.getRepresentative(leftSet.get(i).eval(new Environment())),
                                    qc.getRepresentative(rightSet.get(j).eval(new Environment())),
                                    ljnt);
                        }
                        newlyGeneratedTable.add(ljnt);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return newlyGeneratedTable;
    }
}
