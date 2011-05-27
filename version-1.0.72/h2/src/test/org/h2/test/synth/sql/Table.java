/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License, 
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.synth.sql;

import java.util.ArrayList;

/**
 * Represents a table.
 */
class Table {
    private TestSynth config;
    private String name;
    private boolean temporary;
    private boolean globalTemporary;
    private Column[] columns;
    private Column[] primaryKeys;
    private ArrayList indexes = new ArrayList();

    Table(TestSynth config) {
        this.config = config;
    }

    public static Table newRandomTable(TestSynth config) {
        Table table = new Table(config);
        table.name = "T_" + config.randomIdentifier();

        // there is a difference between local temp tables for persistent and
        // in-memory mode
        // table.temporary = config.random().getBoolean(10);
        // if(table.temporary) {
        // if(config.getMode() == TestSynth.H2_MEM) {
        // table.globalTemporary = false;
        // } else {
        // table.globalTemporary = config.random().getBoolean(50);
        // }
        // }

        int len = config.random().getLog(10) + 1;
        table.columns = new Column[len];
        for (int i = 0; i < len; i++) {
            Column col = Column.getRandomColumn(config);
            table.columns[i] = col;
        }
        if (config.random().getBoolean(90)) {
            int pkLen = config.random().getLog(len);
            table.primaryKeys = new Column[pkLen];
            for (int i = 0; i < pkLen; i++) {
                Column pk = null;
                do {
                    pk = table.columns[config.random().getInt(len)];
                } while (pk.isPrimaryKey());
                table.primaryKeys[i] = pk;
                pk.setPrimaryKey(true);
                pk.setNullable(false);
            }
        }
        return table;
    }

    public Index newRandomIndex() {
        String indexName = "I_" + config.randomIdentifier();
        int len = config.random().getLog(getColumnCount() - 1) + 1;
        boolean unique = config.random().getBoolean(50);
        Column[] cols = getRandomColumns(len);
        Index index = new Index(this, indexName, cols, unique);
        return index;
    }

    public String getDropSQL() {
        return "DROP TABLE " + name;
    }

    public String getCreateSQL() {
        String sql = "CREATE ";
        if (temporary) {
            if (globalTemporary) {
                sql += "GLOBAL ";
            } else {
                sql += "LOCAL ";
            }
            sql += "TEMPORARY ";
        }
        sql += "TABLE " + name + "(";
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sql += ", ";
            }
            Column column = columns[i];
            sql += column.getCreateSQL();
            if (primaryKeys != null && primaryKeys.length == 1 && primaryKeys[0] == column) {
                sql += " PRIMARY KEY";
            }
        }
        if (primaryKeys != null && primaryKeys.length > 1) {
            sql += ", ";
            sql += "PRIMARY KEY(";
            for (int i = 0; i < primaryKeys.length; i++) {
                if (i > 0) {
                    sql += ", ";
                }
                Column column = primaryKeys[i];
                sql += column.getName();
            }
            sql += ")";
        }
        sql += ")";
        return sql;
    }

    public String getInsertSQL(Column[] c, Value[] v) {
        String sql = "INSERT INTO " + name;
        if (c != null) {
            sql += "(";
            for (int i = 0; i < c.length; i++) {
                if (i > 0) {
                    sql += ", ";
                }
                sql += c[i].getName();
            }
            sql += ")";
        }
        sql += " VALUES(";
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sql += ", ";
            }
            sql += v[i].getSQL();
        }
        sql += ")";
        return sql;
    }

    public String getName() {
        return name;
    }

    public Column getRandomConditionColumn() {
        ArrayList list = new ArrayList();
        for (int i = 0; i < columns.length; i++) {
            if (Column.isConditionType(config, columns[i].getType())) {
                list.add(columns[i]);
            }
        }
        if (list.size() == 0) {
            return null;
        }
        return (Column) list.get(config.random().getInt(list.size()));
    }

    public Column getRandomColumn() {
        return columns[config.random().getInt(columns.length)];
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Column getRandomColumnOfType(int type) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].getType() == type) {
                list.add(columns[i]);
            }
        }
        if (list.size() == 0) {
            return null;
        }
        return (Column) list.get(config.random().getInt(list.size()));
    }

    public Column[] getRandomColumns(int len) {
        int[] index = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            index[i] = i;
        }
        for (int i = 0; i < columns.length; i++) {
            int temp = index[i];
            int r = index[config.random().getInt(columns.length)];
            index[i] = index[r];
            index[r] = temp;
        }
        Column[] c = new Column[len];
        for (int i = 0; i < len; i++) {
            c[i] = columns[index[i]];
        }
        return c;
    }

    public Column[] getColumns() {
        return columns;
    }

    public void addIndex(Index index) {
        indexes.add(index);
    }

    public void removeIndex(Index index) {
        indexes.remove(index);
    }
}