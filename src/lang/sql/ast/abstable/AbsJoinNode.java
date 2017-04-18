package lang.sql.ast.abstable;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.datatype.ValType;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.table.TableAttr;
import util.IndentionManagement;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class AbsJoinNode extends AbsTableNode {

    List<AbsTableNode> tableNodes = new ArrayList<AbsTableNode>();

    public AbsJoinNode(List<AbsTableNode> tableNodes) {
        super();
        this.tableNodes = tableNodes;
    }

    public AbsJoinNode(AbsTableNode tn1, AbsTableNode tn2) {
        super();
        this.tableNodes.add(tn1);
        this.tableNodes.add(tn2);
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        List<Table> tables = new ArrayList<Table>();
        for (AbsTableNode tn : tableNodes) {
            tables.add(tn.eval(env));
        }
        Table currentTable = tables.get(0);
        for (int i = 1; i < tables.size(); i ++) {
            currentTable = Table.joinTwo(currentTable, tables.get(i));
        }
        return currentTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        for (AbsTableNode tn : this.tableNodes) {
            schema.addAll(tn.getSchema());
        }
        return schema;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<ValType> valTypes = new ArrayList<ValType>();
        for (AbsTableNode tn : this.tableNodes) {
            valTypes.addAll(tn.getSchemaType());
        }
        return valTypes;
    }

    @Override
    // the level of join is the maximum table level + 1
    public int getNestedQueryLevel() {
        int lv = 0;
        for (AbsTableNode tn : this.tableNodes) {
            if (tn.getNestedQueryLevel() > lv)
                lv = tn.getNestedQueryLevel();
        }
        return lv + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = this.tableNodes.get(0).prettyPrint(1, true).trim();
        for (int i = 1; i < this.tableNodes.size(); i ++) {
            result += " Join \r\n" + this.tableNodes.get(i).prettyPrint(1,true);
        }
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.tableNodes.forEach(tn -> result.addAll(tn.getAllHoles()));
        return result;
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return new AbsJoinNode(
            this.tableNodes.stream()
                .map(t -> t.instantiate(env)).collect(Collectors.toList()));
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return new AbsJoinNode(
                this.tableNodes.stream()
                        .map(t -> t.substNamedVal(vnsb)).collect(Collectors.toList()));    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        List<AbsNamedTable> result = new ArrayList<>();
        for (AbsTableNode t : this.tableNodes) {
            result.addAll(t.namedTableInvolved());
        }
        return result;
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {
        return new AbsJoinNode(
                tableNodes.stream()
                        .map(tn -> tn.tableSubst(pairs))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<String> originalColumnName() {
        List<String> result = new ArrayList<>();
        for (AbsTableNode tn : this.tableNodes) {
            result.addAll(tn.originalColumnName());
        }
        return result;
    }

    public List<AbsTableNode> getTableNodes() { return this.tableNodes; }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        for (AbsTableNode tn : tableNodes) {
            result.addAll(tn.getAllConstants());
        }

        return result;
    }

    @Override
    public String getQuerySkeleton() {
        return "(J" + tableNodes.stream().map(tn -> tn.getQuerySkeleton()).reduce("", (x,y)-> (x + " " + y)) + ")";
    }

    public List<Table> getNamedTableInJoin() {
        List<Table> result = new ArrayList<>();
        for (AbsTableNode tn : this.tableNodes) {
            if (tn instanceof AbsNamedTable)
                result.add(((AbsNamedTable) tn).getTable());
            if (tn instanceof AbsJoinNode) {
                result.addAll(((AbsJoinNode) tn).getNamedTableInJoin());
            }
        }
        return result;
    }

    @Override
    public void genConstraints(Context ctx,
                               Solver solver,
                               Map<AbsTableNode, TableAttr> map) {

        // deal with children first
        for (AbsTableNode atn : this.getTableNodes())
            atn.genConstraints(ctx, solver, map);

        ArithExpr r = ctx.mkIntConst(this.getNodeId() + ".r");
        ArithExpr c = ctx.mkIntConst(this.getNodeId() + ".c");
        ArithExpr newVal = ctx.mkIntConst(this.getNodeId() + ".newVal");

        map.put(this, new TableAttr(r, c, newVal));
        List<TableAttr> tableAttrList = this.tableNodes.stream()
                .map(tn -> map.get(tn)).collect(Collectors.toList());

        solver.add(ctx.mkLe(r, tableAttrList.stream().map(attr -> attr.r).reduce((x, y) -> ctx.mkMul(x, y)).get()));
        solver.add(ctx.mkEq(c, tableAttrList.stream().map(attr -> attr.c).reduce((x, y) -> ctx.mkAdd(x, y)).get()));
        solver.add(ctx.mkEq(newVal, tableAttrList.stream().map(attr -> attr.newVal).reduce((x, y) -> ctx.mkAdd(x, y)).get()));
    }
}
