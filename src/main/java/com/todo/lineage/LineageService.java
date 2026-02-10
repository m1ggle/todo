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
            String semanticText = ExpressionColumnExtractor.sanitize(normalized).toLowerCase(Locale.ROOT);
            ColumnRef targetRef = new ColumnRef(targetTable, targetColumn);
            Set<ColumnRef> inputs = ExpressionColumnExtractor.extractColumnRefs(normalized, sourceTable);

            if (containsToken(semanticText, "dictget(")) {
                builder.warn("dictGet detected: mark as EXTERNAL_LINEAGE");
            }
            if (containsWholeWord(semanticText, "final")) {
                builder.warn("FINAL ignored for lineage");
            }
            if (containsToken(semanticText, "arrayjoin(")) {
                builder.warn("ARRAY JOIN detected in expression");
            }

            if (inputs.isEmpty()) {
                builder.add(new ColumnRef("__const__", normalized), targetRef, LineageType.UNKNOWN, normalized);
                continue;
            }

            LineageType type = detectType(semanticText, inputs, targetColumn);
            for (ColumnRef input : inputs) {
                builder.add(input, targetRef, type, normalized);
            }
        }

        return builder.build();
    }

    private LineageType detectType(String semanticText, Set<ColumnRef> inputs, String targetColumn) {
        if (containsToken(semanticText, "dictget(")) {
            return LineageType.EXTERNAL_LINEAGE;
        }
        if (containsToken(semanticText, "arrayjoin(")) {
            return LineageType.DERIVED_ARRAY;
        }
        if (containsToken(semanticText, "sum(") || containsToken(semanticText, "max(") || containsToken(semanticText, "min(")
            || containsToken(semanticText, "count(") || containsToken(semanticText, "avg(")) {
            return LineageType.AGG;
        }

        if (inputs.size() == 1) {
            ColumnRef input = inputs.iterator().next();
            if (input.column().equalsIgnoreCase(targetColumn)
                && semanticText.equals(targetColumn.toLowerCase(Locale.ROOT))) {
                return LineageType.DIRECT;
            }
        }

        return LineageType.DERIVED;
    }

    private String normalizeExpression(String expression) {
        return expression.trim();
    }

    private boolean containsToken(String expression, String token) {
        return expression.contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean containsWholeWord(String text, String word) {
        String needle = word.toLowerCase(Locale.ROOT);
        int idx = text.indexOf(needle);
        while (idx >= 0) {
            boolean leftOk = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1));
            int end = idx + needle.length();
            boolean rightOk = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            idx = text.indexOf(needle, idx + 1);
        }
        return false;
    }
}
