package forward_enumeration.primitive.parameterized;

import forward_enumeration.context.EnumContext;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.ValHole;
import lang.sql.ast.val.ValNode;
import lang.sql.datatype.ValType;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumParamTN {

    public static final int maxParamFilterLength = 2;
    public static int numberOfParams = 2;

    public static List<AbsTableNode> enumParameterizedTableNodes(
            List<AbsTableNode> basicTables,
            List<ValNode> constants,
            int paramNum) {

        numberOfParams = paramNum;

        List<ValType> valueTypes = new ArrayList<>();
        for (AbsTableNode tn : basicTables) {
            for (ValType vt : tn.getSchemaType()) {
                if (!valueTypes.contains(vt))
                    valueTypes.add(vt);
            }
        }
        constants.addAll(ValHole.genHolesWithType(numberOfParams, valueTypes));

        EnumContext ec = new EnumContext();
        ec.setTableNodes(basicTables.stream().map(bt -> RenameTNWrapper.forceRename(bt)).collect(Collectors.toList()));
        ec.setValNodes(constants);
        ec.setMaxFilterLength(maxParamFilterLength);

        // get all parameterized table nodes by enumerating select nodes with ec
        //      and filter away nodes that contains no holes
        List<AbsTableNode> paramterizedTables =
                EnumSelectTableNode.enumSelectNode(ec, true).stream()
                    .filter(tn -> !tn.getAllHoles().isEmpty())
                    .map(tn -> RenameTNWrapper.tryRename(tn))
                    .collect(Collectors.toList());

        return paramterizedTables;
    }
}
