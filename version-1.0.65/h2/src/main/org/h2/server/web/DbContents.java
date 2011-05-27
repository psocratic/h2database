/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.server.web;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.h2.command.Parser;
import org.h2.util.StringUtils;

/**
 * Keeps meta data information about a database.
 * This class is used by the H2 Console.
 */
public class DbContents {
    DbSchema[] schemas;
    DbSchema defaultSchema;
    boolean isOracle, isH2, isPostgreSQL, isMySQL, isDerby, isFirebird, isSQLite;

    void readContents(DatabaseMetaData meta) throws SQLException {
        String prod = StringUtils.toLowerEnglish(meta.getDatabaseProductName());
        isSQLite = prod.indexOf("sqlite") >= 0;
        String url = meta.getURL();
        if (url != null) {
            isH2 = url.startsWith("jdbc:h2:");
            isOracle = url.startsWith("jdbc:oracle:");
            isPostgreSQL = url.startsWith("jdbc:postgresql:");
            // isHSQLDB = url.startsWith("jdbc:hsqldb:");
            isMySQL = url.startsWith("jdbc:mysql:");
            isDerby = url.startsWith("jdbc:derby:");
            isFirebird = url.startsWith("jdbc:firebirdsql:");
        }
        String defaultSchemaName = getDefaultSchemaName(meta);
        String[] schemaNames = getSchemaNames(meta);
        schemas = new DbSchema[schemaNames.length];
        for (int i = 0; i < schemaNames.length; i++) {
            String schemaName = schemaNames[i];
            boolean isDefault = defaultSchemaName == null || defaultSchemaName.equals(schemaName);
            DbSchema schema = new DbSchema(this, schemaName, isDefault);
            if (schema.isDefault) {
                defaultSchema = schema;
            }
            schemas[i] = schema;
            String[] tableTypes = new String[] { "TABLE", "SYSTEM TABLE", "VIEW", "SYSTEM VIEW", "TABLE LINK",
                    "SYNONYM" };
            schema.readTables(meta, tableTypes);
        }
        if (defaultSchema == null) {
            String best = null;
            for (int i = 0; i < schemas.length; i++) {
                if ("dbo".equals(schemas[i].name)) {
                    // MS SQL Server
                    defaultSchema = schemas[i];
                    break;
                }
                if (defaultSchema == null || best == null || schemas[i].name.length() < best.length()) {
                    best = schemas[i].name;
                    defaultSchema = schemas[i];
                }
            }
        }
    }

    private String[] getSchemaNames(DatabaseMetaData meta) throws SQLException {
        if (isMySQL) {
            return new String[] { "" };
        } else if (isFirebird) {
            return new String[] { null };
        }
        ResultSet rs = meta.getSchemas();
        ArrayList schemas = new ArrayList();
        while (rs.next()) {
            String schema = rs.getString("TABLE_SCHEM");
            if (schema == null) {
                continue;
            }
            schemas.add(schema);
        }
        rs.close();
        String[] list = new String[schemas.size()];
        schemas.toArray(list);
        return list;
    }

    private String getDefaultSchemaName(DatabaseMetaData meta) throws SQLException {
        String defaultSchemaName = "";
        try {
            if (isOracle) {
                return meta.getUserName();
            } else if (isPostgreSQL) {
                return "public";
            } else if (isMySQL) {
                return "";
            } else if (isDerby) {
                return StringUtils.toUpperEnglish(meta.getUserName());
            } else if (isFirebird) {
                return null;
            }
            ResultSet rs = meta.getSchemas();
            int index = rs.findColumn("IS_DEFAULT");
            while (rs.next()) {
                if (rs.getBoolean(index)) {
                    defaultSchemaName = rs.getString("TABLE_SCHEM");
                }
            }
        } catch (SQLException e) {
            // IS_DEFAULT not found
        }
        return defaultSchemaName;
    }

    String quoteIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        if (isH2) {
            return Parser.quoteIdentifier(identifier);
        } else {
            return StringUtils.toUpperEnglish(identifier);
        }
    }

}