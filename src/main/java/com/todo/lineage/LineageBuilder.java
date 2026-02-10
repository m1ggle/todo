package com.todo.lineage;

public final class LineageBuilder {
    private final LineageGraph graph = new LineageGraph();

    public LineageBuilder add(ColumnRef from, ColumnRef to, LineageType type, String expression) {
        graph.addEdge(new LineageEdge(from, to, type, expression));
        return this;
    }

    public LineageBuilder warn(String warning) {
        graph.addWarning(warning);
        return this;
    }

    public LineageGraph build() {
        return graph;
    }
}
