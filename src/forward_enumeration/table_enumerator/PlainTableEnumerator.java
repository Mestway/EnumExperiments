package forward_enumeration.table_enumerator;

import forward_enumeration.AbstractTableEnumerator;
import forward_enumeration.enum_abstract.components.EnumAggrTableNode;
import forward_enumeration.enum_abstract.components.EnumJoinTableNodes;
import forward_enumeration.container.MemQueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.EnumSelectTableNode;
import lang.sql.ast.abstable.AbsTableNode;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumerator extends AbstractTableEnumerator {
    @Override
    public List<AbsTableNode> enumTable(EnumContext ec, int depth) {

        List<AbsTableNode> result = new ArrayList<>();

        MemQueryContainer qc = MemQueryContainer.initWithInputTables(ec.getInputs(), MemQueryContainer.ContainerType.None);
        List<AbsTableNode> agrTables = EnumAggrTableNode.enumAggrNodeWFilter(ec);
        qc.insertQueries(agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            List<AbsTableNode> tableNodes = EnumJoinTableNodes.enumJoinWithoutFilter(ec).stream()
                    .map(jn -> (AbsTableNode) jn).collect(Collectors.toList());
            qc.insertQueries(tableNodes.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            tableNodes = EnumSelectTableNode.enumSelectNode(ec);
            qc.insertQueries(tableNodes);
            result.addAll(tableNodes);
        }

        return result;
    }
}
