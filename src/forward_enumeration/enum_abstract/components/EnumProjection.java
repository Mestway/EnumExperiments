package forward_enumeration.enum_abstract.components;

import backward_inference.CellToCellMap;
import backward_inference.MappingInference;
import forward_enumeration.container.MemQueryContainer;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsSelectNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.NamedVal;
import lang.sql.ast.val.ValNode;
import lang.sql.exception.SQLEvalException;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumerate projection table nodes given the enumeration context EC
 * Created by clwang on 1/26/16.
 */
public class EnumProjection {

    // projection enumeration only happens at the last step
    public static List<AbsTableNode> enumProjection(List<AbsTableNode> tableNodes, Table outputTable) {

        List<AbsTableNode> result = new ArrayList<>();

        for (AbsTableNode tn : tableNodes) {

            Table t;
            try {
                t = tn.eval(new Environment());
                // when the table row size does't equal to the size of output table,
                // it can not be obtained from projection.
                if (t.getContent().size() != outputTable.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                continue;
            }

            MappingInference mi = MappingInference.buildMapping(t, outputTable);
            List<CellToCellMap> maps = mi.genMappingInstances();

            List<List<ValNode>> lvns =  new ArrayList<>();
            for (CellToCellMap m : maps) {
                List<ValNode> selectNodes = new ArrayList<>();
                for (int j = 0; j < m.getMap().get(0).size(); j ++) {
                    selectNodes.add(new NamedVal(tn.getSchema().get(m.getMap().get(0).get(j).c())));
                }
                lvns.add(selectNodes);
            }

            for (List<ValNode> lvn : lvns) {
                AbsSelectNode sn = new AbsSelectNode(lvn, tn);
                try {
                    Table tsn = sn.eval(new Environment());
                    if (tsn.contentStrictEquals(outputTable)) {
                        result.add(sn);
                    }
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    // projection enumeration only happens at the last step
    // the return value identifies whether the table is a runner-up
    public static boolean emitEnumProjection(List<AbsTableNode> tableNodes, Table outputTable, MemQueryContainer qc) {

        boolean findone = false;

        List<AbsTableNode> result = new ArrayList<>();

        for (AbsTableNode tn : tableNodes) {

            Table t;
            try {
                t = tn.eval(new Environment());
                if (t.getContent().size() != outputTable.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                continue;
            }

            // Using coordinate map based inference to efficiently maintain result.
            MappingInference mi = MappingInference.buildMapping(t, outputTable);
            List<CellToCellMap> maps = mi.genMappingInstances();

            List<List<ValNode>> lvns =  new ArrayList<>();
            for (CellToCellMap m : maps) {
                List<ValNode> selectNodes = new ArrayList<>();
                for (int j = 0; j < m.getMap().get(0).size(); j ++)
                    selectNodes.add(new NamedVal(tn.getSchema().get(m.getMap().get(0).get(j).c())));
                lvns.add(selectNodes);
            }

            if (lvns.size() > 0) {
                findone = true;
            }

            for (List<ValNode> lvn : lvns) {
                AbsSelectNode sn = new AbsSelectNode(lvn, tn);
                try {
                    Table tsn = sn.eval(new Environment());
                    if (tsn.contentStrictEquals(outputTable)) {
                        result.add(sn);
                        qc.insertQuery(sn);
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(t),
                                qc.getRepresentative(tsn));
                    }
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return findone;
    }

    // Enumerate all possible combinations of selection fields of a select query
    private static List<List<ValNode>> enumSelectArgs(AbsTableNode tableNode, boolean enumStar) {
        List<ValNode> vals = new ArrayList<ValNode>();

        // collect table column names from the schema
        vals.addAll(tableNode.getSchema().stream()
                .map(s -> new NamedVal(s)).collect(Collectors.toList()));

        List<List<ValNode>> valNodes = new ArrayList<>();
        if (! enumStar)
            valNodes = CombinationGenerator.genCombination(vals);
        else
            valNodes.add(vals);

        return valNodes;
    }

}
