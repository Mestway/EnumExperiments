package forward_enumeration.enum_abstract.components;

import forward_enumeration.container.MemQueryContainer;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsRenameNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.abstable.AbsUnionNode;
import lang.sql.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 10/22/16.
 */
public class EnumUnion {

    public static List<AbsTableNode> enumUnion(List<AbsTableNode> leftSet, List<AbsTableNode> rightSet) {

        List<AbsTableNode> result = new ArrayList<>();

        for (AbsTableNode ti : leftSet) {
            for (AbsTableNode tj : rightSet) {

                List<AbsTableNode> tns = Arrays.asList(ti, tj);

                AbsUnionNode jn = new AbsUnionNode(tns);

                Table t1 = null, t2 = null;

                try {
                    t1 = tns.get(0).eval(new Environment());
                    t2 = tns.get(1).eval(new Environment());
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                if (! Table.schemaMatch(t1, t2))
                    continue;

                AbsRenameNode rt = (AbsRenameNode) RenameTNWrapper.tryRename(jn);
                result.add(rt);
            }
        }
        return result;
    }

    public static List<Table> emitEnumerateUnion(List<AbsTableNode> leftSet,
                                                 List<AbsTableNode> RightSet,
                                                 MemQueryContainer qc) {

        List<Table> newlyGeneratedTables = new ArrayList<>();

        for (AbsTableNode ti : leftSet) {
            for (AbsTableNode tj : RightSet) {

                List<AbsTableNode> tns = Arrays.asList(ti, tj);

                AbsUnionNode jn = new AbsUnionNode(tns);

                Table t1 = null, t2 = null;

                try {
                    t1 = qc.getRepresentative(tns.get(0).eval(new Environment()));
                    t2 = qc.getRepresentative(tns.get(1).eval(new Environment()));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                if (! Table.schemaMatch(t1, t2))
                    continue;


                AbsRenameNode rt = (AbsRenameNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                qc.insertQuery(rt);

                try {
                    Table rtt = rt.eval(new Environment());
                    qc.getTableLinks().insertEdge(t1, t2, qc.getRepresentative(rtt));
                    newlyGeneratedTables.add(qc.getRepresentative(rtt));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }
        return newlyGeneratedTables;
    }
}
