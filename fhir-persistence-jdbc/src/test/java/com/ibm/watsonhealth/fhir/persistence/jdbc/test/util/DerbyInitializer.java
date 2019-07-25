/**
 * (C) Copyright IBM Corp. 2017,2018,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.persistence.jdbc.test.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.ibm.watsonhealth.database.utils.api.IDatabaseTranslator;
import com.ibm.watsonhealth.database.utils.derby.DerbyMaster;
import com.ibm.watsonhealth.database.utils.derby.DerbyPropertyAdapter;
import com.ibm.watsonhealth.database.utils.derby.DerbyTranslator;
import com.ibm.watsonhealth.fhir.persistence.jdbc.exception.FHIRPersistenceDBConnectException;
import com.ibm.watsonhealth.fhir.persistence.jdbc.util.DerbyBootstrapper;

/**
 * This utility class initializes and bootstraps a FHIR Derby database for unit testing. 
 * If an existing database is found in the target path, it is reused. If not, a new database is defined and the appropriate DDL for
 * the FHIR schema is applied.
 * 
 * It's intended that this class be consumed by testng tests in the fhir-persistence-jdbc project.
 * @author markd
 *
 */
public class DerbyInitializer {

    // All tests this same database, which we only have to bootstrap once
    private static final String DB_NAME = "derby/fhirDB";
    
    // The translator to help us out with Derby urls/syntax/exceptions
    private static final IDatabaseTranslator DERBY_TRANSLATOR = new DerbyTranslator();
    
    private Properties dbProps;
    
    /**
     * Main method to facilitate standalone testing of this class.
     * @param args
     */
    public static void main(String[] args) {
        DerbyInitializer initializer = new DerbyInitializer();
        try {
            initializer.bootstrapDb();
        } 
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a new DerbyInitializer using default database properties.
     */
    public DerbyInitializer() {
        super();
        this.dbProps = new Properties();
    }
    
    /**
     * Constructs a new DerbyInitializer using the passed database properties.
     */
    public DerbyInitializer(Properties props) {
        super();
        this.dbProps = props;
    }

    /**
     * Default bootstrap of the database. Does not drop/rebuild.
     * @throws FHIRPersistenceDBConnectException
     * @throws SQLException
     */
    public void bootstrapDb() throws FHIRPersistenceDBConnectException, SQLException {
        bootstrapDb(false);
    }
    
    /**
     * Establishes a connection to fhirDB. Creates the database if necessary complete with tables indexes.
     * @throws FHIRPersistenceDBConnectException
     * @throws SQLException 
     */
    public void bootstrapDb(boolean reset) throws FHIRPersistenceDBConnectException, SQLException {
        final String adminSchemaName = "FHIR_ADMIN";
        final String dataSchemaName = "FHIRDATA";
        
        if (reset) {
            // wipes the disk content of the database
            DerbyMaster.dropDatabase(DB_NAME);
        }

        // Inject the DB_NAME into the dbProps
        DerbyPropertyAdapter adapter = new DerbyPropertyAdapter(dbProps);
        adapter.setDatabase(DB_NAME);

        try (Connection connection = DriverManager.getConnection(DERBY_TRANSLATOR.getUrl(dbProps) + ";create=true")) {
            connection.setAutoCommit(false);
            DerbyBootstrapper.bootstrap(connection, adminSchemaName, dataSchemaName);
        }
        catch (SQLException x) {
            throw DERBY_TRANSLATOR.translate(x);
        } 
    }

    /**
     * Get a connection to an established database
     * Autocommit is disabled (of course)
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DERBY_TRANSLATOR.getUrl(dbProps));
        connection.setAutoCommit(false);
        return connection;
    }
    
}
