package forward_enumeration.container;

import forward_enumeration.enum_abstract.datastructure.TableLinks;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsTableNode;
import lang.table.Table;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 * The container for storing queries
 */
public class SimpleQueryContainer implements QueryContainer {

    Set<AbsTableNode> container = new HashSet<>();
    int visitedQuery = 0;
    int collectedQueries = 0;

    public SimpleQueryContainer() {}

    @Override
    public void collectQueries(Collection<AbsTableNode> tns,
                               Function<AbsTableNode, Boolean> f) {
        this.visitedQuery += tns.size();
        //container.addAll(tns.stream().filter(tn -> f.apply(tn)).collect(Collectors.toSet()));
        for (AbsTableNode atn : tns) {
            if (f.apply(atn)) {
                container.add(atn);
                this.collectedQueries += 1;
            }
        }
    }

    // insert more queries into the QueryChest
    // (these new tables will be used later)

    @Override
    public List<AbsTableNode> getCollectedQueries() {
        return this.container.stream().collect(Collectors.toList());
    }

    @Override
    public String printStatus() {
        return "[#Collected] " + this.collectedQueries
                + "\n" + "[#Visited] " + this.visitedQuery;
    }

}
