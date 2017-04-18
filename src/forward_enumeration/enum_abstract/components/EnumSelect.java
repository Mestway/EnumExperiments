package forward_enumeration.enum_abstract.components;

import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsSelectNode;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.NamedVal;
import lang.sql.ast.val.ValNode;
import lang.sql.datatype.ValType;
import lang.sql.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerate filtered named tables from enum context
 * Created by clwang on 2/1/16.
 */
public class EnumSelect {

    /**
     * Enumerate filter nodes for named tables: given a named table, we will generate filter for the named tables.
     * @return
     */
    public static List<AbsTableNode> enumFilterNamed(EnumContext ec) {
        List<AbsTableNode> targets = ec.getTableNodes().stream()
                .filter(tn -> (tn instanceof AbsNamedTable))
                .collect(Collectors.toList());

        List<AbsTableNode> result = new ArrayList<>();

        for (AbsTableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            // we allow using exists when enumerating filters for a named table.
            boolean allowExists = true;

            AbsTableNode sn = new AbsSelectNode(vals, tn);
            result.add(sn);
        }
        return result;
    }

    // Emit enumerated query on the fly, whether to store them or not is determined by qc
    public static List<Table> emitEnumFilterNamed(EnumContext ec, MemQueryContainer qc) {

        Set<Table> newlyGeneratedTables = new HashSet<>();

        List<AbsTableNode> targets = ec.getTableNodes();

        for (AbsTableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            AbsTableNode sn = new AbsSelectNode(vals, tn);
            // when a table is generated, emit it to the query chest
            // inserting an edge from eval(tn) --> eval(sn)

            if (qc.getContainerType() == MemQueryContainer.ContainerType.TableLinks) {

                // if qc use filter links, we can put filter links into qc
                try {
                    Table src = tn.eval(new Environment());
                    Table dst = sn.eval(new Environment());

                    if (src.getContent().size() == 0 || dst.getContent().size() == 0)
                        continue;

                    qc.insertQuery(RenameTNWrapper.tryRename(sn));
                    qc.getTableLinks().insertEdge(
                            qc.getRepresentative(src),
                            qc.getRepresentative(dst));

                    newlyGeneratedTables.add(qc.getRepresentative(dst));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }


        return newlyGeneratedTables.stream().collect(Collectors.toList());
    }
}
