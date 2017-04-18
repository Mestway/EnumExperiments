package main;

import lang.table.Table;
import lang.table.TableRow;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsAggrNode;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsRenameNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.datatype.NumberVal;
import lang.sql.datatype.StringVal;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import forward_enumeration.primitive.AggrEnumerator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 11/1/16.
 */
public class SynthesizerHelper {

    // Given the input-output example, guess possible extra constants that may be used in query formulation
    public static Set<NumberVal> guessExtraConstants(List<Function<List<Value>, Value>> aggrFunctions,
                                                     List<Table> input) {

        Set<NumberVal> iSet = new HashSet<>();

        int maxInputTableSize = input.stream().map(i -> i.getContent().size()).reduce(0, (x,y)->(x>y?x:y));

        if (aggrFunctions.contains(AbsAggrNode.AggrCount)
                && aggrFunctions.contains(AbsAggrNode.AggrMax))  {

            //This is typically common for max-count / min-count,
            // so we would add some extra constants for the ease of synthesis
            for (AbsTableNode tn : input.stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toSet())) {
                List<AbsRenameNode> countResult = AggrEnumerator
                        .enumerateAggregation(Arrays.asList(AbsAggrNode.AggrCount), tn, true);
                for (AbsTableNode ttn : countResult) {
                    List<AbsRenameNode> maxResult = AggrEnumerator
                            .enumerateAggregation(Arrays.asList(AbsAggrNode.AggrMax), ttn, true);
                    for (AbsTableNode mr : maxResult) {
                        try {
                            Table t = mr.eval(new Environment());
                            if (t.getContent().size() == 1 && (t.getContent().get(0).getValue(0) instanceof NumberVal)
                                    && ((NumberVal)t.getContent().get(0).getValue(0)).getVal() <= maxInputTableSize) {
                                iSet.add(((NumberVal)t.getContent().get(0).getValue(0)));
                            }
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return iSet;
    }

    // Given the input-output example, guess functions that will potentially be used in synthesis
    public static List<Set<Function<List<Value>, Value>>> getRelatedFunctions(List<Value> constValues,
                                                                              List<Table> inputs,
                                                                              Table output) {

        List<Set<Function<List<Value>, Value>>> result = new ArrayList<>();

        Set<Value> valuesInInput = new HashSet<>();
        Set<Value> outputAndConstValues = new HashSet<>();
        int maxColumnCnt = inputs.stream().map(t -> t.getContent().size()).reduce(Integer::max).get();

        for (Table t : inputs) {
            for (TableRow tr : t.getContent()) {
                for (Value v :  tr.getValues()) {
                    valuesInInput.add(v);
                }
            }
        }

        for (TableRow tr : output.getContent()) {
            for (Value v :  tr.getValues()) {
                outputAndConstValues.add(v);
            }
        }

        outputAndConstValues.addAll(constValues);

        List<Value> otherValues = new ArrayList<>();
        for (Value v : outputAndConstValues) {
            if (! valuesInInput.contains(v))
                otherValues.add(v);
        }
        if (! otherValues.isEmpty()) {

            // check whether extra string value exists in output but not in input
            boolean containsExtraStringVal = false;
            for (Value v : otherValues) {
                if (v instanceof StringVal) {
                    containsExtraStringVal = true;
                }
            }
            if (containsExtraStringVal) {
                Set<Function<List<Value>, Value>> tmp = new HashSet<>();
                tmp.add(AbsAggrNode.AggrConcat);
                tmp.add(AbsAggrNode.AggrConcat2);
                result.add(tmp);
            }

            // check whether small values exists in the output example
            boolean containsSmallVal = false;
            for (Value v : otherValues) {
                if (v instanceof NumberVal && ((double)v.getVal() <= maxColumnCnt))
                    containsSmallVal = true;
            }
            if (containsSmallVal) {
                Set<Function<List<Value>, Value>> tmp = new HashSet<>();
                tmp.add(AbsAggrNode.AggrCount);
                tmp.add(AbsAggrNode.AggrCountDistinct);
                result.add(tmp);
            }

            Set<Function<List<Value>, Value>> tmp = new HashSet<>();
            tmp.add(AbsAggrNode.AggrSum);
            result.add(tmp);
        }

        Set<Function<List<Value>, Value>> tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrMax);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrMin);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrAvg);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrMin);
        tmp.add(AbsAggrNode.AggrMax);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrCount);
        tmp.add(AbsAggrNode.AggrMax);
        result.add(tmp);

        return result;
    }

    // Given the input-output example, rank the aggregation functions to try
    public static List<Set<Function<List<Value>, Value>>> rankAggrFunctions(List<Value> constValues, List<Table> input, Table output) {

        List<Set<Function<List<Value>, Value>>> result = getRelatedFunctions(constValues, input, output);

        Set<Function<List<Value>, Value>> tmp = new HashSet<>();
        tmp.add(AbsAggrNode.AggrCount);
        tmp.add(AbsAggrNode.AggrSum);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.addAll(AbsAggrNode.getAllAggrFunctions());
        tmp.remove(AbsAggrNode.AggrConcat);
        tmp.remove(AbsAggrNode.AggrConcat2);
        result.add(tmp);
        return result;
    }

    //Rank the candidates and returns only top k of them
    public static List<AbsTableNode> findTopK(List<AbsTableNode> candidates, int k) {
        if (candidates.isEmpty())
            return candidates;
        else
            return candidates.subList(0, candidates.size() > k ? k : candidates.size());
    }

}
