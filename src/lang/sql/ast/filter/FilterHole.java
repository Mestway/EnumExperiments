package lang.sql.ast.filter;

import lang.sql.ast.Environment;
import lang.sql.datatype.Value;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/10/16.
 * TODO: not yet implemented
 */
public class FilterHole implements Filter, Hole {

    // the hole does not really filter anything
    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return true;
    }

    @Override
    public int getFilterLength() {
        return 0;
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public String prettyPrint(int indentLv) {
        return null;
    }

    @Override
    public boolean containRedundantFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return Arrays.asList(this);
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
