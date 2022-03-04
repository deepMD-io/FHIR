/*
 * (C) Copyright IBM Corp. 2017, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.test;

import com.ibm.fhir.persistence.FHIRPersistence;
import com.ibm.fhir.persistence.FHIRPersistenceFactory;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.search.util.SearchUtil;


/**
 * Mock persistence factory for use during testing.
 */
public class MockPersistenceFactory implements FHIRPersistenceFactory {

    @Override
    public FHIRPersistence getInstance(SearchUtil searchHelper) throws FHIRPersistenceException {
        return new MockPersistenceImpl();
    }
}
