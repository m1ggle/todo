package com.todo.lineage;

import java.util.Objects;

public final class LineageEdge {
    private final ColumnRef from;
    private final ColumnRef to;
    private final LineageType type;
    private final String expression;

    public LineageEdge(ColumnRef from, ColumnRef to, LineageType type, String expression) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.expression = expression;
    }

    public ColumnRef from() {
        return from;
    }

    public ColumnRef to() {
        return to;
    }

    public LineageType type() {
        return type;
    }

    public String expression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LineageEdge)) {
            return false;
        }
        LineageEdge that = (LineageEdge) o;
        return Objects.equals(from, that.from)
            && Objects.equals(to, that.to)
            && type == that.type
            && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type, expression);
    }

    @Override
    public String toString() {
        return "LineageEdge{" +
            "from=" + from +
            ", to=" + to +
            ", type=" + type +
            ", expression='" + expression + '\'' +
            '}';
    }
}
