package lang.sql.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/4/16.
 */
public class EmptyFilter implements Filter {

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return true;
    }

    @Override
    public int getFilterLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public String prettyPrint(int indentLv) {
        return "";
    }

    @Override
    public boolean containRedundantFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    @Override
    public List<Value> getAllConstatnts() {
        return new ArrayList<>();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return this;
    }
}
