package com.todo.lineage;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LineageServiceTestMain {
    public static void main(String[] args) {
        testQualifiedColumns();
        testConstantExpression();
        testArrayJoinType();
        System.out.println("All tests passed.");
    }

    private static void testQualifiedColumns() {
        LineageService service = new LineageService();
        Map<String, String> projection = new LinkedHashMap<>();
        projection.put("c", "t1.a + t2.b");

        LineageGraph graph = service.buildProjectLineage("default_t", "result", projection);

        assertTrue(graph.edges().stream().anyMatch(e -> e.from().toString().equals("t1.a")), "Missing t1.a edge");
        assertTrue(graph.edges().stream().anyMatch(e -> e.from().toString().equals("t2.b")), "Missing t2.b edge");
    }

    private static void testConstantExpression() {
        LineageService service = new LineageService();
        Map<String, String> projection = new LinkedHashMap<>();
        projection.put("flag", "1");

        LineageGraph graph = service.buildProjectLineage("t", "result", projection);
        assertTrue(graph.edges().stream().anyMatch(e -> e.type() == LineageType.UNKNOWN), "Constant should be UNKNOWN");
    }

    private static void testArrayJoinType() {
        LineageService service = new LineageService();
        Map<String, String> projection = new LinkedHashMap<>();
        projection.put("a", "arrayJoin(arr)");

        LineageGraph graph = service.buildProjectLineage("t", "result", projection);
        assertTrue(graph.edges().stream().anyMatch(e -> e.type() == LineageType.DERIVED_ARRAY), "arrayJoin should be DERIVED_ARRAY");
        assertTrue(graph.warnings().stream().anyMatch(w -> w.contains("ARRAY JOIN")), "arrayJoin should emit warning");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
