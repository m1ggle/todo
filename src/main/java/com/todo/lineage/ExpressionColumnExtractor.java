package com.todo.lineage;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpressionColumnExtractor {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern QUALIFIED = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Pattern SINGLE_QUOTED = Pattern.compile("'[^']*'");

    private ExpressionColumnExtractor() {
    }

    public static Set<String> extractColumnNames(String expression) {
        Set<String> result = new LinkedHashSet<>();
        for (ColumnRef ref : extractColumnRefs(expression, "__unknown__")) {
            result.add(ref.column());
        }
        return result;
    }

    public static Set<ColumnRef> extractColumnRefs(String expression, String defaultTable) {
        Set<ColumnRef> result = new LinkedHashSet<>();
        String sanitized = sanitize(expression);

        Matcher qualifiedMatcher = QUALIFIED.matcher(sanitized);
        while (qualifiedMatcher.find()) {
            String table = qualifiedMatcher.group(1);
            String column = qualifiedMatcher.group(2);
            if (!isReserved(table.toLowerCase(Locale.ROOT)) && !isReserved(column.toLowerCase(Locale.ROOT))) {
                result.add(new ColumnRef(table, column));
            }
        }

        String withoutQualified = QUALIFIED.matcher(sanitized).replaceAll(" ");
        Matcher identifierMatcher = IDENTIFIER.matcher(withoutQualified);
        while (identifierMatcher.find()) {
            String token = identifierMatcher.group();
            String lower = token.toLowerCase(Locale.ROOT);
            if (isReserved(lower) || isNumeric(token)) {
                continue;
            }
            result.add(new ColumnRef(defaultTable, token));
        }

        return result;
    }

    private static String sanitize(String expression) {
        String withoutQuotes = SINGLE_QUOTED.matcher(expression).replaceAll(" ");
        return withoutQuotes.replace('`', ' ');
    }

    private static boolean isReserved(String token) {
        return token.equals("sum")
            || token.equals("max")
            || token.equals("min")
            || token.equals("count")
            || token.equals("avg")
            || token.equals("if")
            || token.equals("toint64")
            || token.equals("touint64")
            || token.equals("dictget")
            || token.equals("case")
            || token.equals("when")
            || token.equals("then")
            || token.equals("else")
            || token.equals("end")
            || token.equals("and")
            || token.equals("or")
            || token.equals("not")
            || token.equals("null")
            || token.equals("as")
            || token.equals("final")
            || token.equals("arrayjoin");
    }

    private static boolean isNumeric(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return !token.isEmpty();
    }
}
