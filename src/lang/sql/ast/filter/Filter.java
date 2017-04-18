package lang.sql.ast.filter;

import lang.sql.ast.Environment;
import lang.sql.datatype.Value;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;

import java.util.List;

/**
 * Created by clwang on 12/14/15.
 */
public interface Filter {

    // for evaluation
    boolean filter(Environment env) throws SQLEvalException;

    // for calculating the length of the filter
    int getFilterLength();
    int getNestedQueryLevel();

    String prettyPrint(int indentLv);

    // check whether a filter has redundant components given the presence of f, examples are:
    // there exists a BinopFilter that is identical to f
    // there exists a BinopFilter that has same value but contrary operator
    boolean containRedundantFilter(Filter f);
    List<Hole> getAllHoles();
    List<Value> getAllConstatnts();
    Filter instantiate(InstantiateEnv env);
    Filter substNamedVal(ValNodeSubstBinding vnsb);
}
