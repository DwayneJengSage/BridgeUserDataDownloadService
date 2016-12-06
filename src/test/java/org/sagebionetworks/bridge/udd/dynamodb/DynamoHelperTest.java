package org.sagebionetworks.bridge.udd.dynamodb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import org.testng.annotations.Test;

import org.sagebionetworks.bridge.dynamodb.DynamoQueryHelper;
import org.sagebionetworks.bridge.schema.UploadSchema;

public class DynamoHelperTest {
    private static final String DUMMY_FIELD_DEF_LIST_JSON = "[\n" +
            "   {\n" +
            "       \"name\":\"dummy-field\",\n" +
            "       \"type\":\"STRING\"\n" +
            "   }\n" +
            "]";

    @Test
    public void testGetHealthCodeNoCode() {
        // mock health ID table
        Table mockHealthIdTable = mock(Table.class);
        when(mockHealthIdTable.getItem("id", "test-health-id")).thenReturn(null);

        // set up dynamo helper
        DynamoHelper dynamoHelper = new DynamoHelper();
        dynamoHelper.setDdbHealthIdTable(mockHealthIdTable);

        // execute and validate
        assertNull(dynamoHelper.getHealthCodeFromHealthId("test-health-id"));
    }

    @Test
    public void testGetHealthCode() {
        // mock health ID table
        Item mockItem = new Item().withString("code", "test-health-code");
        Table mockHealthIdTable = mock(Table.class);
        when(mockHealthIdTable.getItem("id", "test-health-id")).thenReturn(mockItem);

        // set up dynamo helper
        DynamoHelper dynamoHelper = new DynamoHelper();
        dynamoHelper.setDdbHealthIdTable(mockHealthIdTable);

        // execute and validate
        assertEquals(dynamoHelper.getHealthCodeFromHealthId("test-health-id"), "test-health-code");
    }

    @Test
    public void testGetStudy() {
        // mock study table
        Item mockItem = new Item().withString("name", "Test Study").withString("stormpathHref", "dummy-stormpath-href")
                .withString("supportEmail", "support@sagebase.org");
        Table mockStudyTable = mock(Table.class);
        when(mockStudyTable.getItem("identifier", "test-study")).thenReturn(mockItem);

        // set up dynamo helper
        DynamoHelper dynamoHelper = new DynamoHelper();
        dynamoHelper.setDdbStudyTable(mockStudyTable);

        // execute and validate
        StudyInfo studyInfo = dynamoHelper.getStudy("test-study");
        assertEquals(studyInfo.getStudyId(), "test-study");
        assertEquals(studyInfo.getName(), "Test Study");
        assertEquals(studyInfo.getStormpathHref(), "dummy-stormpath-href");
        assertEquals(studyInfo.getSupportEmail(), "support@sagebase.org");
    }

    @Test
    public void testGetSynapseSurveyTables() {
        // mock Synapse survey table
        Item mockItem = new Item().withString("studyId", "test-study").withStringSet("tableIdSet", "foo-table",
                "bar-table");
        Table mockSynapseSurveyTable = mock(Table.class);
        when(mockSynapseSurveyTable.getItem("studyId", "test-study")).thenReturn(mockItem);

        // set up dynamo helper
        DynamoHelper helper = new DynamoHelper();
        helper.setDdbSynapseSurveyTablesTable(mockSynapseSurveyTable);

        // execute and validate
        Set<String> tableIdSet = helper.getSynapseSurveyTablesForStudy("test-study");
        assertEquals(tableIdSet.size(), 2);
        assertTrue(tableIdSet.contains("foo-table"));
        assertTrue(tableIdSet.contains("bar-table"));
    }

    @Test
    public void testGetSynapseSurveyTablesNoTableIds() {
        // mock Synapse survey table
        Item mockItem = new Item().withString("studyId", "test-study");
        Table mockSynapseSurveyTable = mock(Table.class);
        when(mockSynapseSurveyTable.getItem("studyId", "test-study")).thenReturn(mockItem);

        // set up dynamo helper
        DynamoHelper helper = new DynamoHelper();
        helper.setDdbSynapseSurveyTablesTable(mockSynapseSurveyTable);

        // execute and validate
        Set<String> tableIdSet = helper.getSynapseSurveyTablesForStudy("test-study");
        assertTrue(tableIdSet.isEmpty());
    }

    @Test
    public void testGetSynapseSurveyTablesNoItem() {
        // mock Synapse survey table
        Table mockSynapseSurveyTable = mock(Table.class);
        when(mockSynapseSurveyTable.getItem("studyId", "test-study")).thenReturn(null);

        // set up dynamo helper
        DynamoHelper helper = new DynamoHelper();
        helper.setDdbSynapseSurveyTablesTable(mockSynapseSurveyTable);

        // execute and validate
        Set<String> tableIdSet = helper.getSynapseSurveyTablesForStudy("test-study");
        assertTrue(tableIdSet.isEmpty());
    }

    @Test
    public void testGetSynapseTablesAndSchemas() throws Exception {
        // There are 3 sub-cases to test here
        // * foo schema has no table
        // * bar schema has a table
        // * qwerty and asdf schemas both point to the same table

        DynamoHelper dynamoHelper = new DynamoHelper();

        // Mock Schema table Study index. This involves stubbing out queryHelper() because indices can't be mocked
        // directly.
        List<Item> mockSchemaStudyIndexResult = new ArrayList<>();
        mockSchemaStudyIndexResult.add(makeUploadSchemaDdbItem("test-study", "foo", 1, null));
        mockSchemaStudyIndexResult.add(makeUploadSchemaDdbItem("test-study", "bar", 2, null));
        mockSchemaStudyIndexResult.add(makeUploadSchemaDdbItem("test-study", "qwerty", 3, null));
        mockSchemaStudyIndexResult.add(makeUploadSchemaDdbItem("test-study", "asdf", 4, null));

        Index mockSchemaStudyIndex = mock(Index.class);
        DynamoQueryHelper mockQueryHelper = mock(DynamoQueryHelper.class);
        when(mockQueryHelper.query(mockSchemaStudyIndex, "studyId", "test-study"))
                .thenReturn(mockSchemaStudyIndexResult);
        dynamoHelper.setDdbUploadSchemaStudyIndex(mockSchemaStudyIndex);
        dynamoHelper.setQueryHelper(mockQueryHelper);

        // mock schema table
        Table mockSchemaTable = mock(Table.class);
        when(mockSchemaTable.getItem("key", "test-study:foo", "revision", 1)).thenReturn(makeUploadSchemaDdbItem(
                "test-study", "foo", 1, DUMMY_FIELD_DEF_LIST_JSON));
        when(mockSchemaTable.getItem("key", "test-study:bar", "revision", 2)).thenReturn(makeUploadSchemaDdbItem(
                "test-study", "bar", 2, DUMMY_FIELD_DEF_LIST_JSON));
        when(mockSchemaTable.getItem("key", "test-study:qwerty", "revision", 3)).thenReturn(makeUploadSchemaDdbItem(
                "test-study", "qwerty", 3, DUMMY_FIELD_DEF_LIST_JSON));
        when(mockSchemaTable.getItem("key", "test-study:asdf", "revision", 4)).thenReturn(makeUploadSchemaDdbItem(
                "test-study", "asdf", 4, DUMMY_FIELD_DEF_LIST_JSON));
        dynamoHelper.setDdbUploadSchemaTable(mockSchemaTable);

        // mock synapse map table
        Table mockSynapseMapTable = mock(Table.class);
        when(mockSynapseMapTable.getItem("schemaKey", "test-study-bar-v2")).thenReturn(makeSynapseMapDdbItem(
                "test-study-bar-v2", "bar-table-id"));
        when(mockSynapseMapTable.getItem("schemaKey", "test-study-qwerty-v3")).thenReturn(makeSynapseMapDdbItem(
                "test-study-qwerty-v3", "qwerty-asdf-table-id"));
        when(mockSynapseMapTable.getItem("schemaKey", "test-study-asdf-v4")).thenReturn(makeSynapseMapDdbItem(
                "test-study-asdf-v4", "qwerty-asdf-table-id"));
        dynamoHelper.setDdbSynapseMapTable(mockSynapseMapTable);

        // execute and validate - Just check the key equals the schema we expect. Deep validation of schemas is done
        // in the schema tests
        Map<String, UploadSchema> synapseToSchemaMap = dynamoHelper.getSynapseTableIdsForStudy("test-study");
        assertEquals(synapseToSchemaMap.size(), 2);
        assertEquals(synapseToSchemaMap.get("bar-table-id").getKey().toString(), "test-study-bar-v2");
        assertEquals(synapseToSchemaMap.get("qwerty-asdf-table-id").getKey().toString(), "test-study-asdf-v4");
    }

    private static Item makeUploadSchemaDdbItem(String studyId, String schemaId, int rev, String fieldDefListJson) {
        Item retval = new Item().withString("studyId", studyId).withString("key", studyId + ":" + schemaId)
                .withInt("revision", rev);
        if (fieldDefListJson != null) {
            retval.withString("fieldDefinitions", fieldDefListJson);
        }
        return retval;
    }

    private static Item makeSynapseMapDdbItem(String schemaKey, String synapseTableId) {
        return new Item().withString("schemaKey", schemaKey).withString("tableId", synapseTableId);
    }
}
