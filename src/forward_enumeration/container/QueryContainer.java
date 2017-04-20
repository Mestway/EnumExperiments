package forward_enumeration.container;

import lang.sql.ast.abstable.AbsTableNode;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * Created by clwang on 4/14/17.
 */
public interface QueryContainer {
    void collectQueries(Collection<AbsTableNode> tns, Function<AbsTableNode, Boolean> f);
    List<AbsTableNode> getCollectedQueries();
    String printStatus();
}