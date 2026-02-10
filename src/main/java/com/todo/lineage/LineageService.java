package com.todo.lineage;

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
            ColumnRef targetRef = new ColumnRef(targetTable, targetColumn);
            Set<String> inputCols = ExpressionColumnExtractor.extractColumnNames(expression);

            if (expression.toLowerCase().contains("dictget(")) {
                builder.warn("dictGet detected: mark as EXTERNAL_LINEAGE");
            }
            if (expression.toLowerCase().contains("final")) {
                builder.warn("FINAL ignored for lineage");
            }

            if (inputCols.isEmpty()) {
                builder.add(new ColumnRef("__const__", expression), targetRef, LineageType.UNKNOWN, expression);
                continue;
            }

            LineageType type = detectType(expression, inputCols, targetColumn);
            for (String col : inputCols) {
                builder.add(new ColumnRef(sourceTable, col), targetRef, type, expression);
            }
        }

        return builder.build();
    }

    private LineageType detectType(String expression, Set<String> inputCols, String targetColumn) {
        String lower = expression.toLowerCase();

        if (lower.contains("dictget(")) {
            return LineageType.EXTERNAL_LINEAGE;
        }
        if (lower.contains("sum(") || lower.contains("max(") || lower.contains("min(") || lower.contains("count(")) {
            return LineageType.AGG;
        }
        if (inputCols.size() == 1 && inputCols.contains(targetColumn) && lower.equals(targetColumn.toLowerCase())) {
            return LineageType.DIRECT;
        }
        return LineageType.DERIVED;
    }
}
