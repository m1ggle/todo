package com.todo.lineage;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DemoMain {
    public static void main(String[] args) {
        LineageService service = new LineageService();

        Map<String, String> projection = new LinkedHashMap<>();
        projection.put("user_id", "user_id");
        projection.put("total_amt", "sum(amount)");
        projection.put("max_ts", "max(toUInt64(ts))");
        projection.put("dim_name", "dictGet('dim_user', 'name', user_id)");
        projection.put("pair_metric", "t1.a + t2.b");
        projection.put("item", "arrayJoin(items)");
        projection.put("const_one", "1");

        LineageGraph graph = service.buildProjectLineage("ods_orders", "result", projection);

        System.out.println("Nodes:");
        for (ColumnRef node : graph.nodes()) {
            System.out.println("- " + node);
        }

        System.out.println("\nEdges:");
        for (LineageEdge edge : graph.edges()) {
            System.out.println("- " + edge);
        }

        System.out.println("\nWarnings:");
        for (String warning : graph.warnings()) {
            System.out.println("- " + warning);
        }

        System.out.println("\nJSON:");
        System.out.println(graph.toJson());
    }
}
