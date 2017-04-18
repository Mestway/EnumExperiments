package forward_enumeration.container;

import forward_enumeration.enum_abstract.datastructure.TableLinks;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsNamedTable;
import lang.sql.ast.abstable.AbsTableNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 * The container for storing queries
 */
public class MemQueryContainer implements QueryContainer {

    @Override
    public void collectQueries(Collection<AbsTableNode> tns, Function<AbsTableNode, Boolean> f) {
        this.insertQueries(tns.stream().filter(x -> f.apply(x)).collect(Collectors.toList()));
    }

    @Override
    public List<AbsTableNode> getCollectedQueries() {
        return this.getRepresentativeTableNodes();
    }

    @Override
    public String printStatus() {
        return "[#Representatives] " + this.getRepresentativeTableNodes().size();
    }

    public enum ContainerType { SummaryTableWBV, TableLinks, None }

    ContainerType containerType = ContainerType.None;
    public ContainerType getContainerType() { return this.containerType; }

    // store the getRepresentative table of tables with the same content, to ensure that hash lookup will not mess it up
    private Map<Table, Table> mirror = new HashMap<>();

    public Set<Table> getMemoizedTables() { return this.mirror.keySet(); }

    private MemQueryContainer() {}

    public MemQueryContainer(ContainerType containerType) {
        this.containerType = containerType;
    }

    public static MemQueryContainer initWithInputTables(List<Table> input, ContainerType containerType) {
        MemQueryContainer qc = new MemQueryContainer();
        qc.containerType = containerType;
        qc.insertQueries(input.stream().map(t -> new AbsNamedTable(t)).collect(Collectors.toList()));
        return qc;
    }

    public Set<AbsTableNode> dumbCollector = new HashSet<>();
    public void dumbInsertQuerie(Collection<AbsTableNode> tn) {
        dumbCollector.addAll(tn);
    }
    public int visitedQuery = 0;

    public void insertQuery(AbsTableNode tn) {
        try {
            Table t = tn.eval(new Environment());

            if (t.getContent().size() == 0)
                return;

            if (! mirror.containsKey(t))
                mirror.put(t, t);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // insert more queries into the QueryChest
    // (these new tables will be used later)
    public void insertQueries(Collection<AbsTableNode> queries) {
        for (AbsTableNode tn : queries) {
            insertQuery(tn);
        }
    }

    public List<AbsTableNode> getRepresentativeTableNodes() {
        return this.mirror.entrySet().stream().map(p -> new AbsNamedTable(p.getValue())).collect(Collectors.toList());
    }

    public Table getRepresentative(Table t) {
        return mirror.get(t);
    }

    // the data structure to store what are the ways to generate one table from other tables.
    // this data structure is updated during each enumeration module
    private TableLinks tableLinks = new TableLinks();
    public TableLinks getTableLinks() { return this.tableLinks; }

}
