
/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.bulkdata.provider.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.AppendBlobClient;
import com.ibm.fhir.bulkdata.dto.ReadResultDTO;
import com.ibm.fhir.bulkdata.jbatch.export.data.ExportTransientUserData;
import com.ibm.fhir.bulkdata.jbatch.load.data.ImportTransientUserData;
import com.ibm.fhir.bulkdata.jbatch.load.exception.FHIRLoadException;
import com.ibm.fhir.bulkdata.provider.Provider;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.model.util.FHIRUtil;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationFactory;

/**
 * AzureProvider integrates the BulkData feature with Azure Blob Storage.
 * The code uses the Blob Clients.
 *
 * $export: The AppendBlobClient is used during upload to append to an export file.
 * $import: The BlobClient with BlobRange is used to retrieve windows of data.
 *
 * @see  https://docs.microsoft.com/en-us/dotnet/api/azure.storage.blobs.specialized.appendblobclient?view=azure-dotnet
 */
public class AzureProvider implements Provider {
    private static final Logger LOG = Logger.getLogger(AzureProvider.class.getName());

    private static final byte[] NEWLINE = "\r\n".getBytes();

    private ImportTransientUserData transientUserData = null;
    private ExportTransientUserData chunkData = null;

    private String cosBucketPathPrefix = null;
    private String fhirResourceType = null;

    private List<Resource> resources = new ArrayList<>();

    private long parseFailures = 0;
    private long currentBytes = 0;

    private String connectionString;
    private String container;

    private String workItem;

    private boolean collect = false;

    private BlobClient client;

    /**
     * Configures the Azure based on the storageProvider source
     * @param source
     */
    public AzureProvider(String source) {
        this.connectionString = ConfigurationFactory.getInstance().getStorageProviderAuthTypeConnectionString(source);
        this.container = ConfigurationFactory.getInstance().getStorageProviderBucketName(source);
        this.collect = ConfigurationFactory.getInstance().shouldStorageProviderCollectOperationOutcomes(source);
    }

    /**
     * registers the overrides for testing.
     * @param connectionString
     * @param container the container (also known as a bucket)
     * @param chunkData the export data
     * @param cosBucketPathPrefix path prefix
     * @param fhirResourceType resource type
     * @param currentBytes the long value or location of the end of the last resource
     */
    protected void registerOverride(String connectionString, String container, ExportTransientUserData chunkData,
        String cosBucketPathPrefix, String fhirResourceType, long currentBytes) {
        this.connectionString = connectionString;
        this.container = container;
        this.chunkData = chunkData;
        this.cosBucketPathPrefix = cosBucketPathPrefix;
        this.fhirResourceType = fhirResourceType;
        this.currentBytes = currentBytes;
    }

    /**
     * initializes the blob client
     * @param workItem
     */
    protected void initializeBlobClient(String workItem) {
        if (client == null) {
            client = new BlobClientBuilder()
                .connectionString(connectionString)
                .containerName(container)
                .blobName(workItem)
                .buildClient();
        }
    }

    @Override
    public void createSource() throws FHIRException {
        try {
            BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(container)
                .buildClient();
            blobContainerClient.create();
            LOG.info("Container is created '" + container + "'");
        } catch (BlobStorageException error) {
            LOG.fine("Can't create container. It already exists");
            LOG.throwing(this.getClass().getName(), "createSource", error);
        }
    }

    @Override
    public long getSize(String workItem) throws FHIRException {
        try {
            initializeBlobClient(workItem);
            long size = client.getProperties().getBlobSize();
            LOG.fine(() -> workItem + " [" + size + "]");
            return size;
        } catch (Exception e) {
            LOG.throwing("AzureProvider", "getSize", e);
            LOG.fine("Error Getting File Size '" + container + "/" + workItem + "'");
            throw new FHIRLoadException("Error Getting File Size '" + container + "/" + workItem + "'");
        }
    }

    /**
     * lists the blobs on the container.
     * @throws FHIRException
     */
    public void listBlobsForContainer() throws FHIRException {
        try {
            BlobContainerClient containerClient = new BlobContainerClientBuilder()
                    .connectionString(connectionString)
                    .containerName(container)
                    .buildClient();
            LOG.info("Listing the blobs");
            containerClient.listBlobs().forEach(blob -> LOG.info("BLOB: " + blob.getName()));
            LOG.info("Finished listing the blobs");
        } catch (Exception e) {
            throw new FHIRException("Error listing the blobs for '" + container + "'", e);
        }
    }

    /**
     * writes directly to Azure using AppendBlock client
     * @param workItem the file
     * @param in
     * @param size
     * @throws Exception
     */
    public void writeDirectly(String workItem, InputStream in, int size) throws Exception{
        initializeBlobClient(workItem);

        AppendBlobClient aClient = client.getAppendBlobClient();
        if (!client.exists().booleanValue()) {
            aClient.create();
        }
        // Append a new line.
        aClient.appendBlock(in, size);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(NEWLINE)) {
            aClient.appendBlock(bais, NEWLINE.length);
        }
    }

    @Override
    public void writeResources(String mediaType, List<ReadResultDTO> dtos) throws Exception {
        String workItem = cosBucketPathPrefix + "/" + fhirResourceType + "_" + chunkData.getUploadCount() + ".ndjson";

        initializeBlobClient(workItem);

        AppendBlobClient aClient = client.getAppendBlobClient();
        if (!client.exists().booleanValue()) {
            aClient.create();
        }
        aClient.appendBlock(
            new ByteArrayInputStream(chunkData.getBufferStream().toByteArray()),
                chunkData.getBufferStream().size());

        chunkData.getBufferStream().reset();

        if (chunkData.isFinishCurrentUpload()) {
            // Partition status for the exported resources, e.g, Patient[1000,1000,200]
            if (chunkData.getResourceTypeSummary() == null) {
                chunkData.setResourceTypeSummary(fhirResourceType + "[" + chunkData.getCurrentUploadResourceNum());
                if (chunkData.getPageNum() >= chunkData.getLastPageNum()) {
                    chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
                }
            } else {
                chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "," + chunkData.getCurrentUploadResourceNum());
                if (chunkData.getPageNum() >= chunkData.getLastPageNum()) {
                    chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
                }
            }

            if (chunkData.getPageNum() < chunkData.getLastPageNum()) {
                chunkData.setPartNum(1);
                chunkData.setUploadId(null);
                chunkData.setCurrentUploadResourceNum(0);
                chunkData.setCurrentUploadSize(0);
                chunkData.setFinishCurrentUpload(false);
                chunkData.getCosDataPacks().clear();
                chunkData.setUploadCount(chunkData.getUploadCount() + 1);
            }
        }
    }

    /*
     * We process the BlobRange as a window
     */
    @Override
    public void readResources(long numOfLinesToSkip, String workItem) throws FHIRException {
        initializeBlobClient(workItem);
        this.workItem = workItem;

        boolean complete = false;
        int window = 0;

        // 200 means 2000M! has been retrieved, our maximum single resource size.
        StringBuilder previous = new StringBuilder();
        while (!complete && window < 200) {
            BlobRequestConditions options = new BlobRequestConditions();
            BlobRange range = new BlobRange(currentBytes, 10000L);
            // Now we need to calculate where we left off in the download, and adjust the resume point.
            try (InputStream in = client.openInputStream(range, options);
                    CountInputStreamReader counter = new CountInputStreamReader(in);
                    BufferedReader reader = new BufferedReader(counter)) {

                List<String> lines = reader.lines().collect(Collectors.toList());
                int idx = 1;
                for (String line : lines) {
                    idx++;
                    String compLine = previous + line;

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(compLine.getBytes())) {
                        resources.add(FHIRParser.parser(Format.JSON).parse(bais));
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Azure Resource [R] " + compLine);
                        }
                        // There is no reason to reset compLine as we're now stopping this proccessing block.
                        // previous, baos are all going to go out of scope.
                        if (window > 0) {
                            // Doesn't need to be closed per JavaDocs
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            baos.write(line.getBytes());
                            currentBytes += baos.size();
                            complete = true;
                            break;
                        }
                    } catch(FHIRParserException fpe) {
                        // We're on the last line, so there might be more
                        if (idx -1 == lines.size()) {
                            previous.append(line);
                        } else {
                            parseFailures++;
                            OperationOutcome.Issue ooi = FHIRUtil.buildOperationOutcomeIssue("Failed to Process " + this.transientUserData.getNumOfProcessedResources() + idx, IssueType.EXCEPTION);
                            OperationOutcome oo = OperationOutcome.builder().issue(ooi).build();
                            FHIRGenerator.generator(Format.JSON).generate(oo, this.transientUserData.getBufferStreamForImportError());
                            this.transientUserData.getBufferStreamForImportError().write("\r\n".getBytes());
                        }
                    }
                }
                if (!complete) {
                    currentBytes += counter.getLength();
                    window++;
                }

                // Set so we can move the window forward.
                this.transientUserData.setCurrentBytes(currentBytes);
                LOG.fine(() -> "Number of bytes read are: '" + currentBytes + "'");
            } catch (IOException e) {
                LOG.throwing("AzureProvider", "readResources", e);
                LOG.severe("Problem accessing backend on Azure" + e.getMessage());
                throw new FHIRException("Problem accessing backend Container or Blob on Azure", e);
            }
        }
    }

    /**
     * CountInputStreamReader counts the size of the input.
     */
    private static class CountInputStreamReader extends InputStreamReader {
        private long l = 0;

        public CountInputStreamReader(InputStream in) {
            super(in);
        }

        @Override
        public int read(char[] cbuf, int offset, int length) throws IOException {
            int r = super.read(cbuf, offset, length);
            if (r != -1) {
                l += r;
            }
            return r;
        }

        /**
         * @return the length of the resources returned in the reader
         */
        public long getLength() {
            return l;
        }
    }

    @Override
    public void pushOperationOutcomes() throws FHIRException {
        if (collect && this.transientUserData.getBufferStreamForImportError().size() != 0) {
            // Push the error
            String workItemError = this.workItem + "_oo_errors.ndjson";
            LOG.fine(() -> "Outputting the error to " +  workItemError);
            initializeBlobClient(workItemError);

            AppendBlobClient aClient = client.getAppendBlobClient();
            if (!client.exists().booleanValue()) {
                aClient.create();
            }
            aClient.appendBlock(
                new ByteArrayInputStream(this.transientUserData.getBufferStreamForImportError().toByteArray()),
                    this.transientUserData.getBufferStreamForImportError().size());

            this.transientUserData.getBufferStreamForImportError().reset();
            // Push the success
            // @implNote Sadly we don't do this right now.
        }
    }


    @Override
    public void close() throws Exception {
        // No Operation
    }

    @Override
    public List<Resource> getResources() throws FHIRException {
        return resources;
    }

    @Override
    public long getNumberOfParseFailures() throws FHIRException {
        return parseFailures;
    }

    @Override
    public long getNumberOfLoaded() throws FHIRException {
        return resources.size();
    }

    @Override
    public void registerTransient(ImportTransientUserData transientUserData) {
        this.transientUserData = transientUserData;
        this.currentBytes = transientUserData.getCurrentBytes();
    }

    @Override
    public void registerTransient(long executionId, ExportTransientUserData transientUserData, String cosBucketPathPrefix, String fhirResourceType) throws Exception {
        if (transientUserData == null) {
            LOG.warning("registerTransient: chunkData is null, this should never happen!");
            throw new Exception("registerTransient: chunkData is null, this should never happen!");
        }

        this.cosBucketPathPrefix = cosBucketPathPrefix;
        this.fhirResourceType = fhirResourceType;

        this.chunkData = transientUserData;
    }
}