package lang.sql.ast.abstable;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.datatype.ValType;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;
import lang.table.Table;
import lang.table.TableAttr;
import util.IndentionManagement;
import util.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/13/17.
 */
public class AbsProjNode extends AbsTableNode {

    AbsTableNode tableNode;

    public AbsProjNode(AbsTableNode atn) {
        super();
        this.tableNode = atn;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        return this.tableNode.eval(new Environment());
    }

    @Override
    public List<String> getSchema() {
        return this.tableNode.getSchema();
    }

    @Override
    public String getTableName() {
        return tableNode.getTableName();
    }

    @Override
    public List<ValType> getSchemaType() {
        return tableNode.getSchemaType();
    }

    @Override
    public int getNestedQueryLevel() {
        return this.tableNode.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = "Proj" + this.tableNode.prettyPrint(1, true).trim();
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.tableNode.getAllHoles();
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return this.tableNode.instantiate(env);
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return this.tableNode.substNamedVal(vnsb);
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        return this.tableNode.namedTableInvolved();
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode, AbsTableNode>> pairs) {
        return this.tableNode.tableSubst(pairs);
    }

    @Override
    public List<String> originalColumnName() {
        return this.tableNode.originalColumnName();
    }

    @Override
    public List<Value> getAllConstants() {
        return this.tableNode.getAllConstants();
    }

    @Override
    public void genConstraints(Context ctx, Solver solver, Map<AbsTableNode, TableAttr> map) {

        // emit constraints from the child first
        this.tableNode.genConstraints(ctx, solver, map);

        ArithExpr r = ctx.mkIntConst(this.getNodeId() + ".r");
        ArithExpr c = ctx.mkIntConst(this.getNodeId() + ".c");
        ArithExpr newVal = ctx.mkIntConst(this.getNodeId() + ".newVal");

        map.put(this, new TableAttr(r, c, newVal));
        TableAttr innerTableAttr = map.get(tableNode);

        solver.add(ctx.mkEq(r, innerTableAttr.r));
        solver.add(ctx.mkLe(c, innerTableAttr.c));
        solver.add(ctx.mkLe(newVal, innerTableAttr.newVal));
    }

    @Override
    public String getQuerySkeleton() {
        return "(Proj " + this.tableNode.getQuerySkeleton() + ")";
    }
}
