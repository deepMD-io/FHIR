/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.operation.bulkdata.processor;

import static com.ibm.fhir.model.type.String.string;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.config.FHIRConfiguration;
import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.patch.FHIRPatch;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.operation.bulkdata.BulkDataConstants.ExportType;
import com.ibm.fhir.operation.bulkdata.config.cache.BulkDataTenantSpecificCache;
import com.ibm.fhir.operation.bulkdata.processor.impl.DummyImportExportImpl;
import com.ibm.fhir.operation.context.FHIROperationContext;
import com.ibm.fhir.persistence.FHIRPersistenceTransaction;
import com.ibm.fhir.rest.FHIRResourceHelpers;
import com.ibm.fhir.rest.FHIRRestOperationResponse;

/**
 * 
 */
public class DummyImportExportImplTest {

    @BeforeClass
    public void setup() {
        FHIRConfiguration.setConfigHome("target/test-classes/testdata");
    }

    @Test(expectedExceptions = { FHIROperationException.class })
    public void testExportBulkDataGroupInvalid() throws FHIRException {
        String logicalId = null;
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        FHIRRequestContext.set(new FHIRRequestContext("default"));

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        Parameters parmetersOut =
                impl.export(logicalId, ExportType.GROUP, MediaType.valueOf("application/fhir+ndjson"), Instant.now(),
                        null, null, operationContext, resourceHelper);
        assertNotNull(parmetersOut);
    }

    @Test
    public void testExportBulkDataGroupValidEmpty() throws FHIRException {
        String logicalId = "";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        FHIRRequestContext.set(new FHIRRequestContext("default"));

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        int idx = 1;
        FHIROperationException e = null;
        while (idx != 4) {
            try {
                impl.export(logicalId, ExportType.GROUP, MediaType.valueOf("application/fhir+ndjson"),
                        Instant.now(), null, null, operationContext, resourceHelper);
                break;
            } catch (Exception fe) {
                if (fe instanceof FHIROperationException) {
                    FHIROperationException foe = (FHIROperationException) fe;
                    if (foe.getCause() != null
                            && foe.getCause().getMessage().contains("Group export requires group id!")) {
                        e = foe;
                    }
                }
            }
            idx++;
        }
        assertNotNull(e);
    }

    @Test
    public void testExportBulkDataGroupValidNull() throws FHIRException {
        String logicalId = null;
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        FHIRRequestContext.set(new FHIRRequestContext("default"));

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        int idx = 1;
        FHIROperationException e = null;
        while (idx != 4) {
            try {
                impl.export(logicalId, ExportType.GROUP, MediaType.valueOf("application/fhir+ndjson"),
                        Instant.now(), null, null, operationContext, resourceHelper);
                break;
            } catch (Exception fe) {
                if (fe instanceof FHIROperationException) {
                    FHIROperationException foe = (FHIROperationException) fe;
                    if (foe.getCause() != null
                            && foe.getCause().getMessage().contains("Group export requires group id!")) {
                        e = foe;
                    }
                }
            }
            idx++;
        }
        assertNotNull(e);
    }

    @Test
    public void testExportBulkDataGroupValid() throws FHIRException {
        String logicalId = "3";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        FHIRRequestContext.set(new FHIRRequestContext("default"));

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        int idx = 1;
        while (idx != 4) {
            try {
                impl.export(logicalId, ExportType.GROUP, MediaType.valueOf("application/fhir+ndjson"),
                        Instant.now(), null, null, operationContext, resourceHelper);
                break;
            } catch (Exception e) {
            }
            idx++;
        }
        assertTrue(idx < 4);
    }
    
    @Test
    public void testExportBulkDataSystemValid() throws FHIRException {
        String logicalId = "3";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        FHIRRequestContext.set(new FHIRRequestContext("default"));

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        int idx = 1;
        while (idx != 4) {
            try {
                impl.export(logicalId, ExportType.SYSTEM, MediaType.valueOf("application/fhir+ndjson"),
                        Instant.now(), null, null, operationContext, resourceHelper);
                break;
            } catch (Exception e) {
            }
            idx++;
        }
        assertTrue(idx < 4);
    }

    @Test(expectedExceptions = { FHIROperationException.class })
    public void testImportBulkData() throws FHIROperationException {
        String logicalId = null;
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        FHIRResourceHelpers resourceHelper = generateFHIRResourceHelpers();

        // parameters
        List<Parameter> pss = new ArrayList<>();
        pss.add(Parameter.builder().name(string("job")).value(string("1234q346")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(pss);
        Parameters parameters = builder.build();

        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        impl.importBulkData(logicalId, parameters, operationContext, resourceHelper);
    }

    @Test(expectedExceptions = { FHIROperationException.class })
    public void testDeleteJobFailed() throws FHIROperationException {
        String logicalId = "1";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        impl.delete(logicalId, operationContext);
    }

    @Test
    public void testDeleteJobSuccess() throws FHIROperationException {
        String logicalId = "2";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        Parameters parameters = impl.delete(logicalId, operationContext);
        assertNotNull(parameters);
    }

    @Test(expectedExceptions = { FHIROperationException.class })
    public void testStatusFailed() throws FHIROperationException {
        String logicalId = "2";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        impl.status(logicalId, operationContext);
    }

    @Test
    public void testStatusSuccess() throws FHIROperationException {
        String logicalId = "1";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        Parameters parameters = impl.status(logicalId, operationContext);
        assertNotNull(parameters);
    }

    @Test
    public void testStatusSuccessRetry() throws FHIROperationException {
        String logicalId = "3";
        FHIROperationContext operationContext = FHIROperationContext.createSystemOperationContext();
        BulkDataTenantSpecificCache cache = new BulkDataTenantSpecificCache();
        DummyImportExportImpl impl = new DummyImportExportImpl(cache);
        Parameters parameters = impl.status(logicalId, operationContext);
        assertNotNull(parameters);
    }

    private FHIRResourceHelpers generateFHIRResourceHelpers() {
        return new FHIRResourceHelpers() {

            @Override
            public FHIRRestOperationResponse doCreate(String type, Resource resource, String ifNoneExist,
                    Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public FHIRRestOperationResponse doUpdate(String type, String id, Resource newResource, String ifMatchValue,
                    String searchQueryString, Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public FHIRRestOperationResponse doPatch(String type, String id, FHIRPatch patch, String ifMatchValue,
                    String searchQueryString, Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public FHIRRestOperationResponse doDelete(String type, String id, String searchQueryString,
                    Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public Resource doRead(String type, String id, boolean throwExcOnNull, boolean includeDeleted,
                    Map<String, String> requestProperties, Resource contextResource,
                    MultivaluedMap<String, String> queryParameters) throws Exception {
                return null;
            }

            @Override
            public Resource doRead(String type, String id, boolean throwExcOnNull, boolean includeDeleted,
                    Map<String, String> requestProperties, Resource contextResource) throws Exception {
                return null;
            }

            @Override
            public Resource doVRead(String type, String id, String versionId, Map<String, String> requestProperties)
                    throws Exception {
                return null;
            }

            @Override
            public Bundle doHistory(String type, String id, MultivaluedMap<String, String> queryParameters,
                    String requestUri, Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public Bundle doSearch(String type, String compartment, String compartmentId,
                    MultivaluedMap<String, String> queryParameters, String requestUri,
                    Map<String, String> requestProperties, Resource contextResource) throws Exception {
                return null;
            }

            @Override
            public Resource doInvoke(FHIROperationContext operationContext, String resourceTypeName, String logicalId,
                    String versionId, String operationName, Resource resource,
                    MultivaluedMap<String, String> queryParameters, Map<String, String> requestProperties)
                    throws Exception {
                return null;
            }

            @Override
            public Bundle doBundle(Resource bundle, Map<String, String> requestProperties) throws Exception {
                return null;
            }

            @Override
            public FHIRPersistenceTransaction getTransaction() throws Exception {
                return null;
            }
        };
    }
}