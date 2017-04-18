package lang.sql.ast.filter;

import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import util.IndentionManagement;

import java.util.List;

/**
 * Created by clwang on 12/23/15.
 */
public class ExistsFilter implements Filter {

    // When this flag is true, this existComparator is NOT EXISTS
    private boolean notExists = false;
    AbsTableNode tableNode;

    public ExistsFilter(AbsTableNode tn) {
        this.tableNode = tn;
    }
    public ExistsFilter(AbsTableNode tn, boolean notExists) { this.tableNode = tn; this.notExists = notExists; }

    public AbsTableNode getTableNode() { return this.tableNode; }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (table.getContent().isEmpty())
            return notExists;
        return ! notExists;
    }

    @Override
    public int getFilterLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (notExists == true) {
            return IndentionManagement.addIndention(
                    "Not Exists \r\n" + tableNode.prettyPrint(1, true),
                    indentLv
            );
        }
        return IndentionManagement.addIndention(
                "Exists \r\n" + tableNode.prettyPrint(1, true),
                indentLv
        );
    }

    @Override
    public boolean containRedundantFilter(Filter f) { return false; }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public List<Value> getAllConstatnts() {
        return tableNode.getAllConstants();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new ExistsFilter(this.tableNode.instantiate(env), this.notExists);
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new ExistsFilter(this.tableNode.substNamedVal(vnsb), this.notExists);
    }

}