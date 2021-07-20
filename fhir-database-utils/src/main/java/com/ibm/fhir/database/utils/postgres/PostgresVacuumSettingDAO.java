/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.database.utils.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.database.utils.api.IDatabaseStatement;
import com.ibm.fhir.database.utils.api.IDatabaseTranslator;
import com.ibm.fhir.database.utils.model.DbType;
import com.ibm.fhir.database.utils.model.With;

/**
 * Per the Performance Guide, this DAO implements VACUUM setting changes.
 * https://ibm.github.io/FHIR/guides/FHIRPerformanceGuide/#412-tuning-auto-vacuum
 *
 * Lower the trigger threshold for starting work
 * alter table fhirdata.logical_resources SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_vacuum_threshold=1000);
 *
 * Increase the amount of work vacuuming completes before taking a breather (default is typically 200)
 * alter table fhirdata.logical_resources SET (autovacuum_vacuum_cost_limit=2000);
 */
public class PostgresVacuumSettingDAO implements IDatabaseStatement {

    private static final Logger LOG = Logger.getLogger(PostgresVacuumSettingDAO.class.getName());

    private String schema = null;
    private String tableName = null;

    private int vacuumCostLimit = 0;
    private double vacuumScaleFactor = 0.0;
    private int vacuumThreshold = 0;

    /**
     * sets up the vacuum setting for Postgres
     *
     * @param schema
     * @param tableName
     * @param vacuumCostLimit
     * @param vacuumScaleFactor
     * @param vacuumThreshold
     */
    public PostgresVacuumSettingDAO(String schema, String tableName, int vacuumCostLimit, double vacuumScaleFactor, int vacuumThreshold) {
        this.schema = schema;
        this.tableName = tableName;
        this.vacuumCostLimit = vacuumCostLimit;
        this.vacuumScaleFactor = vacuumScaleFactor;
        this.vacuumThreshold = vacuumThreshold;
    }

    @Override
    public void run(IDatabaseTranslator translator, Connection c) {
        if (translator.getType() == DbType.POSTGRESQL) {
            boolean processSettings1 = true;
            boolean processSettings2 = true;
            boolean processSettings3 = true;
            LOG.fine(() -> "Checking the table vacuum settings");
            final String statement0 = "select (reloptions)::VARCHAR "
                    + "from pg_class "
                    + "join pg_namespace on pg_namespace.oid = pg_class.relnamespace "
                    + "where relname = LOWER('" + tableName + "') "
                    + "and pg_namespace.nspname = LOWER('" + schema + "')";
            try (PreparedStatement ps = c.prepareStatement(statement0)) {
                ps.execute();
                ResultSet rs = ps.getResultSet();
                if (rs.next()) {
                    String settings = rs.getString(1);
                    if (!rs.wasNull()) {
                        String[] sar = settings
                                    .replace("{", "")
                                    .replace("}", "")
                                    .split(",");
                        for (String setting : sar) {
                            String[] kv = setting.split("=");
                            if ("autovacuum_vacuum_scale_factor".equals(kv[0]) && vacuumScaleFactor == Double.parseDouble(kv[1])) {
                                processSettings1 = false;
                            } else if ("autovacuum_vacuum_threshold".equals(kv[0]) && vacuumThreshold == Integer.parseInt(kv[1])) {
                                processSettings2 = false;
                            } else if ("autovacuum_vacuum_cost_limit".equals(kv[0]) && vacuumCostLimit == Integer.parseInt(kv[1])) {
                                processSettings3 = false;
                            }
                        }
                    }
                }
            } catch (SQLException x) {
                throw translator.translate(x);
            }

            // Build the SQL
            StringBuilder builder = new StringBuilder();
            builder.append("alter table ")
                .append(schema).append(".").append(tableName)
                .append(" SET ( ")
                .append(
                    generateWiths(processSettings1, processSettings2, processSettings3).stream()
                        .map(with -> with.buildWithComponent())
                        .collect(Collectors.joining(",")))
                .append(" )");
            if (processSettings1 || processSettings2 || processSettings3) {
                final String statement1 = builder.toString();

                LOG.fine(() -> "Updating the table vacuum settings " + statement1);
                try (PreparedStatement ps = c.prepareStatement(statement1)) {
                    ps.execute();
                } catch (SQLException x) {
                    throw translator.translate(x);
                }

                LOG.info("Completed update for the autovacuum on table '" + schema + "." + tableName + "'");
            }
        }
    }

    /**
     * Selective generation of the settings
     * @param processSettings1
     * @param processSettings2
     * @param processSettings3
     * @return
     */
    private List<With> generateWiths(boolean processSettings1,boolean processSettings2, boolean processSettings3){
        List<With> withs = new ArrayList<>();

        if (processSettings1) {
            withs.add(With.with("autovacuum_vacuum_scale_factor", Integer.toString(vacuumCostLimit)));
        }
        if (processSettings1) {
            withs.add(With.with("autovacuum_vacuum_threshold", Double.toString(vacuumCostLimit)));
        }
        if (processSettings1) {
            withs.add(With.with("autovacuum_vacuum_cost_limit",  Integer.toString(vacuumCostLimit)));
        }
        return withs;
    }
}