package com.todo.lineage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LineageGraph {
    private final Set<ColumnRef> nodes = new LinkedHashSet<>();
    private final Set<LineageEdge> edges = new LinkedHashSet<>();
    private final List<String> warnings = new ArrayList<>();

    public void addEdge(LineageEdge edge) {
        nodes.add(edge.from());
        nodes.add(edge.to());
        edges.add(edge);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public Set<ColumnRef> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<LineageEdge> edges() {
        return Collections.unmodifiableSet(edges);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
