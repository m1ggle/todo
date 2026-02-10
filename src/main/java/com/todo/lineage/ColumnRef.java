package com.todo.lineage;

import java.util.Objects;

public final class ColumnRef {
    private final String table;
    private final String column;

    public ColumnRef(String table, String column) {
        this.table = table;
        this.column = column;
    }

    public String table() {
        return table;
    }

    public String column() {
        return column;
    }

    public String key() {
        return table + "." + column;
    }

    @Override
    public String toString() {
        return key();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColumnRef)) {
            return false;
        }
        ColumnRef that = (ColumnRef) o;
        return Objects.equals(table, that.table) && Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, column);
    }
}
