package forward_enumeration.enum_abstract.datastructure;

import forward_enumeration.enum_abstract.components.OneStepQueryInference;
import forward_enumeration.context.EnumContext;
import global.GlobalConfig;
import lang.table.Table;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsTableNode;
import util.Pair;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The data structure used to store how a table can be generated from other tables.
 *          node
 *          /   \
 *       node1  node2
 * indicates that the 'node' can be generated from node1 and node 2
 * Each table tree corresponds to a bunch of SQL queries to generate the root node table.
 * Created by clwang on 4/7/16.
 */
public class TableTreeNode {

    Table node;
    List<TableTreeNode> children = new ArrayList<>();

    // an uniformed encoding for childrens
    List<AbsTableNode> childrenEncoding = new ArrayList<>();

    // this field is invalid empty until "treeToQuery" is called
    List<AbsTableNode> queries = new ArrayList<>();

    public TableTreeNode(Table node, List<TableTreeNode> children) {
        this.node = node;
        this.children = children;
        this.childrenEncoding = children.stream().map(t -> new AbsNamedTable(t.getTable())).collect(Collectors.toList());
    }

    public Table getTable() {
        return this.node;
    }

    public List<TableTreeNode> getChildren() { return this.children; }

    // test if all of the leaf nodes of the tree are valid.
    public boolean leafValid(Set<Table> validLeaves) {
        if (this.children.size() == 0) {
            return validLeaves.contains(this.node);
        } else {
            boolean valid = true;
            for (TableTreeNode ttn : children) {
                valid = valid && ttn.leafValid(validLeaves);
            }
            return valid;
        }
    }

    public void print(int depth) {
        String indent = "";
        for (int i = 0; i < depth; i ++) {
            indent += "    ";
        }
        System.out.println(this.node.toStringWithIndent(indent));
        for (TableTreeNode ttn : this.children) {
            ttn.print(depth + 1);
        }
    }

    public void inferQuery(EnumContext ec) {

        // this is a leaf node, the query is NamedTable(t)
        if (this.children.size() == 0) {
            this.queries.add(new AbsNamedTable(this.node));
            return;
        }

        ec.setTableNodes(childrenEncoding);
        List<AbsTableNode> tns = OneStepQueryInference.infer(childrenEncoding, this.getTable(), ec);
        this.queries = tns;

        for (TableTreeNode ttn : this.children) {
            ttn.inferQuery(ec);
        }

    }

    public int countQueryNum() {
        // in this case, it is
        int num = this.queries.size();
        for (TableTreeNode ttn : this.children) {
            num = num * ttn.countQueryNum();
        }
        return num;
    }

    // translate an tree to a set of sql queries
    // NOTE: this method can be very expensive,
    // as all possible combinations of generating the query will be expanded
    public List<AbsTableNode> treeToQuery() {

        List<AbsTableNode> result = new ArrayList<>();

        List<List<AbsTableNode>> horizontalSelections = new ArrayList<>();
        horizontalSelections.add(new ArrayList<>());

        for (TableTreeNode ttn : this.children) {
            List<List<AbsTableNode>> newHS = new ArrayList<>();
            List<AbsTableNode> tns = ttn.treeToQuery();
            for (List<AbsTableNode> hs : horizontalSelections) {
                for (AbsTableNode tn : tns) {
                    List<AbsTableNode> updated = new ArrayList<>();
                    updated.addAll(hs);
                    updated.add(tn);
                    newHS.add(updated);
                }
            }
            horizontalSelections = newHS;
        }

        for (AbsTableNode q : this.queries) {
            for (List<AbsTableNode> selection : horizontalSelections) {
                List<Pair<AbsTableNode, AbsTableNode>> substPair = new ArrayList<>();
                for (int i = 0; i < this.childrenEncoding.size(); i ++) {
                    substPair.add(new Pair<>(childrenEncoding.get(i), RenameTNWrapper.tryRename(selection.get(i))));
                }
                result.add(q.tableSubst(substPair));
            }
        }

        if (result.size() > GlobalConfig.MAXIMUM_BEAM_SIZE) {
            return result.subList(0, GlobalConfig.MAXIMUM_BEAM_SIZE);
        }

        return result;
    }

}
