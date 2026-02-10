package com.todo.lineage;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpressionColumnExtractor {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern SINGLE_QUOTED = Pattern.compile("'[^']*'");

    private ExpressionColumnExtractor() {
    }

    public static Set<String> extractColumnNames(String expression) {
        Set<String> result = new LinkedHashSet<>();
        String sanitized = SINGLE_QUOTED.matcher(expression).replaceAll(" ");
        Matcher matcher = IDENTIFIER.matcher(sanitized);

        while (matcher.find()) {
            String token = matcher.group();
            String lower = token.toLowerCase(Locale.ROOT);
            if (isReserved(lower) || isNumeric(token)) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    private static boolean isReserved(String token) {
        return token.equals("sum")
            || token.equals("max")
            || token.equals("min")
            || token.equals("count")
            || token.equals("if")
            || token.equals("toint64")
            || token.equals("touint64")
            || token.equals("dictget")
            || token.equals("case")
            || token.equals("when")
            || token.equals("then")
            || token.equals("else")
            || token.equals("end");
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
