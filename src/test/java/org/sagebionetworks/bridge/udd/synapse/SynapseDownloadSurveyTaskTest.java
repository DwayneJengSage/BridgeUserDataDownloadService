package org.sagebionetworks.bridge.udd.synapse;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.Reader;
import java.io.Writer;

import com.google.common.io.CharStreams;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.sagebionetworks.bridge.udd.helper.InMemoryFileHelper;

public class SynapseDownloadSurveyTaskTest {
    private static final String TEST_FILE_HANDLE = "test-file-handle";
    private static final String TEST_SYNAPSE_TABLE_ID = "test-table";
    private static final String TEST_SYNAPSE_TABLE_NAME = "Test Table";

    private InMemoryFileHelper fileHelper;
    private SynapseHelper synapseHelper;
    private SynapseDownloadSurveyTask task;
    private File tmpDir;

    @BeforeMethod
    public void setup() throws Exception {
        // mock Synapse helper
        synapseHelper = mock(SynapseHelper.class);

        TableEntity table = new TableEntity();
        table.setId(TEST_SYNAPSE_TABLE_ID);
        table.setName(TEST_SYNAPSE_TABLE_NAME);
        when(synapseHelper.getTable(TEST_SYNAPSE_TABLE_ID)).thenReturn(table);

        when(synapseHelper.generateFileHandleFromTableQuery("SELECT * FROM " + TEST_SYNAPSE_TABLE_ID,
                TEST_SYNAPSE_TABLE_ID)).thenReturn(TEST_FILE_HANDLE);

        // create in-memory file helper
        fileHelper = new InMemoryFileHelper();
        tmpDir = fileHelper.createTempDir();

        // create params
        SynapseDownloadSurveyParameters params = new SynapseDownloadSurveyParameters.Builder()
                .withSynapseTableId(TEST_SYNAPSE_TABLE_ID).withTempDir(tmpDir).build();

        // create task
        task = new SynapseDownloadSurveyTask(params);
        task.setFileHelper(fileHelper);
        task.setSynapseHelper(synapseHelper);
    }

    @Test
    public void errorDownloadingFile() throws Exception {
        // set up error
        doThrow(new TestSynapseException()).when(synapseHelper).downloadFileHandle(eq(TEST_FILE_HANDLE),
                notNull(File.class));

        // execute
        Exception thrownEx = null;
        try {
            task.call();
            fail("expected exception");
        } catch (SynapseException ex) {
            thrownEx = ex;
        }
        assertNotNull(thrownEx);

        postValidation();
    }

    @Test
    public void errorPartialDownload() throws Exception {
        // set up error
        doAnswer(invocation -> {
            File targetFile = invocation.getArgumentAt(1, File.class);
            try (Writer targetFileWriter = fileHelper.getWriter(targetFile)) {
                targetFileWriter.write("partial survey content");
            }

            throw new TestSynapseException();
        }).when(synapseHelper).downloadFileHandle(eq(TEST_FILE_HANDLE), notNull(File.class));

        // execute
        Exception thrownEx = null;
        try {
            task.call();
            fail("expected exception");
        } catch (SynapseException ex) {
            thrownEx = ex;
        }
        assertNotNull(thrownEx);

        postValidation();
    }

    @Test
    public void happyCase() throws Exception {
        // set up Synapse helper
        doAnswer(invocation -> {
            File targetFile = invocation.getArgumentAt(1, File.class);
            try (Writer targetFileWriter = fileHelper.getWriter(targetFile)) {
                targetFileWriter.write("dummy survey content");
            }

            // Answer declares return type, even if Void
            return null;
        }).when(synapseHelper).downloadFileHandle(eq(TEST_FILE_HANDLE), notNull(File.class));

        // execute and validate
        File file = task.call();
        try (Reader reader = fileHelper.getReader(file)) {
            assertEquals(CharStreams.toString(reader), "dummy survey content");
        }

        // cleanup/post-validation
        fileHelper.deleteFile(file);
        postValidation();
    }

    // We can't use an AfterMethod, because AfterMethod doesn't report which method failed.
    private void postValidation() throws Exception {
        fileHelper.deleteDir(tmpDir);
        assertTrue(fileHelper.isEmpty());
    }
}
