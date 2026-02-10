package com.todo.lineage;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LineageService {

    public LineageGraph buildProjectLineage(
        String sourceTable,
        String targetTable,
        Map<String, String> targetColumnToExpression
    ) {
        LineageBuilder builder = new LineageBuilder();

        for (Map.Entry<String, String> entry : targetColumnToExpression.entrySet()) {
            String targetColumn = entry.getKey();
            String expression = entry.getValue();
            String normalized = normalizeExpression(expression);
            ColumnRef targetRef = new ColumnRef(targetTable, targetColumn);
            Set<ColumnRef> inputs = ExpressionColumnExtractor.extractColumnRefs(normalized, sourceTable);

            if (containsToken(normalized, "dictget(")) {
                builder.warn("dictGet detected: mark as EXTERNAL_LINEAGE");
            }
            if (containsToken(normalized, "final")) {
                builder.warn("FINAL ignored for lineage");
            }
            if (containsToken(normalized, "arrayjoin(")) {
                builder.warn("ARRAY JOIN detected in expression");
            }

            if (inputs.isEmpty()) {
                builder.add(new ColumnRef("__const__", normalized), targetRef, LineageType.UNKNOWN, normalized);
                continue;
            }

            LineageType type = detectType(normalized, inputs, targetColumn);
            for (ColumnRef input : inputs) {
                builder.add(input, targetRef, type, normalized);
            }
        }

        return builder.build();
    }

    private LineageType detectType(String expression, Set<ColumnRef> inputs, String targetColumn) {
        String lower = expression.toLowerCase(Locale.ROOT);

        if (containsToken(lower, "dictget(")) {
            return LineageType.EXTERNAL_LINEAGE;
        }
        if (containsToken(lower, "arrayjoin(")) {
            return LineageType.DERIVED_ARRAY;
        }
        if (containsToken(lower, "sum(") || containsToken(lower, "max(") || containsToken(lower, "min(")
            || containsToken(lower, "count(") || containsToken(lower, "avg(")) {
            return LineageType.AGG;
        }

        if (inputs.size() == 1) {
            ColumnRef input = inputs.iterator().next();
            if (input.column().equalsIgnoreCase(targetColumn) && lower.equals(targetColumn.toLowerCase(Locale.ROOT))) {
                return LineageType.DIRECT;
            }
        }

        return LineageType.DERIVED;
    }

    private String normalizeExpression(String expression) {
        return expression.trim();
    }

    private boolean containsToken(String expression, String token) {
        return expression.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }
}
