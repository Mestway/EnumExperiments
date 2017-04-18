package forward_enumeration.primitive.parameterized;

import forward_enumeration.context.EnumContext;
import lang.sql.ast.abstable.AbsSelectNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.NamedVal;
import lang.sql.ast.val.ValHole;
import lang.sql.ast.val.ValNode;
import lang.sql.datatype.ValType;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enum a classical Select...From...Where query with given ec
 * Created by clwang on 1/7/16.
 */
public class EnumSelectTableNode {

    /********************************************************************************
     Enum table by select
     1. Enumerate the table to select from
     2. Enumerate fields to be projected
     3. Enumerate filters to perform filtering
     *********************************************************************************/

    public static List<AbsTableNode> enumSelectNode(EnumContext ec) {
        return enumSelectNode(ec, false);
    }

    public static List<AbsTableNode> enumSelectNode(EnumContext ec, boolean selectStar) {

        List<AbsTableNode> result = new ArrayList<>();

        List<AbsTableNode> coreTableNode = ec.getTableNodes(); //TableEnumerator.enumTable(ec, depth - 1);

        for (AbsTableNode tn : coreTableNode) {
            List<List<ValNode>> lvn = enumSelectArgs(ec,tn, selectStar);
            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            for (List<ValNode> vn : lvn) {
                AbsTableNode sn = new AbsSelectNode(vn, tn);
                result.add(sn);
            }
        }
        return result;
    }

    // Enumerate the selection fields of a select query
    private static List<List<ValNode>> enumSelectArgs(EnumContext ec, AbsTableNode tableNode, boolean enumStar) {
        List<ValNode> vals = new ArrayList<ValNode>();
        // TODO: check whether ruling out hole param is a good idea
        vals.addAll(ec.getValNodes().stream()
                .filter(vn -> !(vn instanceof ValHole)).collect(Collectors.toList()));

        // collect table column names from the schema
        vals.addAll(tableNode.getSchema().stream()
                .map(s -> new NamedVal(s)).collect(Collectors.toList()));

        List<List<ValNode>> valNodes = new ArrayList<>();
        if (! enumStar)
            valNodes = CombinationGenerator.genCombination(vals);
        else
            valNodes.add(vals);

        return valNodes;
    }

}
