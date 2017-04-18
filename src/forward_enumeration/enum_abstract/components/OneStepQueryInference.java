package forward_enumeration.enum_abstract.components;

import forward_enumeration.context.EnumContext;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/7/16.
 * this class help up identify this one step query,
 *   when we have already known that we can obtain TableB from TableA (or a set of tables TableA1,TableA2...) in one step,
 */
public class OneStepQueryInference {

    public static List<AbsTableNode> infer(List<AbsTableNode> inputTableNodes, Table output, EnumContext ec) {

        if (inputTableNodes.size() == 1) {

            // if input table nodes contains only one table,
            // then it can either be obtained from select(filter), aggregation or projection

            List<AbsTableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);

            tns.addAll(EnumAggrTableNode.enumAggrNodeWFilter(ec).stream()
                    .filter(tn -> {
                        try {
                            return tn.eval(new Environment()).equals(output);
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }).collect(Collectors.toList()));

            tns.addAll(EnumSelect.enumFilterNamed(ec).stream().filter(tn -> {
                try {
                    return (tn.eval(new Environment()).equals(output));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            tns.addAll(EnumProjection.enumProjection(ec.getTableNodes(), output));

            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec).stream().filter(tn -> {
                try {
                    return (tn.eval(new Environment()).equals(output));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            return tns;

        } else if (inputTableNodes.size() == 2) {

            // if the output table is generated from 2 input tables, the only way is by join (for now)

            List<AbsTableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);

            // possibly from join
            tns.addAll(EnumJoinTableNodes.enumJoinLeftRight(inputTableNodes, inputTableNodes, ec).stream().filter(t -> {
                try {
                    return t.eval(new Environment()).equals(output);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            // avoid overlap between leftjoin and join
            if (tns.isEmpty()) {
                // possibly from left-join
                tns.addAll(EnumLeftJoin.enumLeftJoin(ec).stream().filter(t -> {
                    try {
                        return t.eval(new Environment()).equals(output);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).collect(Collectors.toList()));
            }

            tns.addAll(EnumUnion.enumUnion(inputTableNodes, inputTableNodes  ).stream().filter(t -> {
                try {
                    return t.eval(new Environment()).equals(output);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            return tns;
        }
        return new ArrayList<>();
    }
}
