package lang.sql.datatype;

import lang.sql.ast.abstable.AbsAggrNode;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by clwang on 4/3/17.
 */
public class LazyAggrVal implements Value {

    // This value represents a list of values,

    Function<List<Value>, Value> f;
    // note that we cannot use set to store this, since values may be used up to many times by the aggregation function
    List<Value> components = new ArrayList<>();

    public LazyAggrVal(List<Value> vals, Function<List<Value>, Value> f) {
        this.components.addAll(vals.stream().map(x -> x.duplicate()).collect(Collectors.toList()));
        this.f = f;
    }

    @Override
    public Object getVal() {
        return components;
    }

    @Override
    public Value duplicate() {
        return new LazyAggrVal(components, f);
    }

    @Override
    public ValType getValType() {
        return f.apply(components).getValType();
    }

    public List<Value> getComponents() { return this.components; }

    public CombinedVal instantiateToCombinedVal() {

        if (f == AbsAggrNode.AggrMax || f == AbsAggrNode.AggrMin) {
            return new CombinedVal(components.stream().collect(Collectors.toSet()));
        }

        if (f == AbsAggrNode.AggrCount) {
            return new CombinedVal(IntStream.range(0, this.components.size())
                    .mapToObj(i -> new NumberVal(i)).collect(Collectors.toSet()));
        }

        if (f == AbsAggrNode.AggrCountDistinct) {
            return new CombinedVal(IntStream.range(0, this.components.stream().collect(Collectors.toSet()).size())
                    .mapToObj(i -> new NumberVal(i)).collect(Collectors.toSet()));
        }

        // this one is dumb
        return new CombinedVal(CombinationGenerator.genCombination(components)
                                .stream()
                                .map(l -> f.apply(l))
                                .distinct()
                                .collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + AbsAggrNode.FuncName(f) + ") [" + this.components.stream()
                .map(x -> x.toString()).reduce("", (x, y)-> x + ", " + y).substring(2) + "]";
    }

    @Override
    public int hashCode() {
        int result = this.f.hashCode();
        for (Value v : this.components) {
            result = (result + v.hashCode()) % 1300127;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LazyAggrVal) {
            return this.components.containsAll(((LazyAggrVal) o).components)
                    && ((LazyAggrVal) o).components.containsAll(this.components) && this.f.equals(((LazyAggrVal) o).f);
        }
        return false;
    }

    // this is a simple but dumb implementation of the approach
    public boolean existsAnInstantiationTo(Value val) {
        return this.instantiateToCombinedVal().existsAnInstantiationTo(val);
    }

}
