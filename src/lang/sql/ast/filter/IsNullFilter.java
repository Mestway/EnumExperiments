package lang.sql.ast.filter;

import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.val.ValNode;
import lang.sql.datatype.NullVal;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 10/17/16.
 */
public class IsNullFilter implements Filter {

    // If this flag is set to true, the filter is actually IS NOT NULL xxx
    boolean negSign = false;
    // the column to be tested
    ValNode arg;

    public IsNullFilter(ValNode arg, boolean negSign) {
        this.arg = arg;
        this.negSign = negSign;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        Value v1 = arg.eval(env);
        if (v1 instanceof NullVal)
            return ! negSign;
        return negSign;
    }

    @Override
    public int getFilterLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return Math.max(arg.getNestedQueryLevel(), 0);
    }

    @Override
    public String prettyPrint(int indentLv) {
        String str = arg.prettyPrint(0) + " Is" + (negSign ? " Not " : " ") + "NULL";
        return IndentionManagement.addIndention(str, indentLv);
    }

    @Override
    public boolean containRedundantFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return arg.getAllHoles();
    }

    @Override
    public List<Value> getAllConstatnts() {
        return new ArrayList<>();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new IsNullFilter(arg.instantiate(env), this.negSign);
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new IsNullFilter(arg.subst(vnsb), this.negSign);
    }
}
