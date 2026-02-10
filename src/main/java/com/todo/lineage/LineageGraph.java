package com.todo.lineage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LineageGraph {
    private final Set<ColumnRef> nodes = new LinkedHashSet<>();
    private final Set<LineageEdge> edges = new LinkedHashSet<>();
    private final Set<String> warnings = new LinkedHashSet<>();

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
        return Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"nodes\": [");

        int i = 0;
        for (ColumnRef n : nodes) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(escape(n.toString())).append("\"");
        }

        sb.append("],\n  \"edges\": [\n");

        int j = 0;
        for (LineageEdge e : edges) {
            if (j++ > 0) {
                sb.append(",\n");
            }
            sb.append("    {\"from\": \"").append(escape(e.from().toString()))
                .append("\", \"to\": \"").append(escape(e.to().toString()))
                .append("\", \"type\": \"").append(e.type())
                .append("\", \"expr\": \"").append(escape(e.expression())).append("\"}");
        }

        sb.append("\n  ],\n  \"warnings\": [");

        int k = 0;
        for (String w : warnings) {
            if (k++ > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(escape(w)).append("\"");
        }

        sb.append("]\n}");
        return sb.toString();
    }

    private String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
