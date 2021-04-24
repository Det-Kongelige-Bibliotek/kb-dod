package dk.kb.dod;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import dk.kb.alma.gen.*;
import dk.kb.alma.gen.additional.Holdings;
import dk.statsbiblioteket.util.xml.DOM;
import org.marc4j.marc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


public class AlmaClient {
    private static final Logger log = LoggerFactory.getLogger(AlmaClient.class);
    private final WebResource resource;
    private final String apikey;

    static final String DF245_TAG = "245"; // Title
    static final String DF500_TAG = "500"; // Note field
    static final String DF035_TAG = "035"; // Network number
    static final String DF260_TAG = "260";
    static final String DF300_TAG = "300";
    static final String DF090_TAG = "090";
    static final String DF091_TAG = "091";
    static final String DF599_TAG = "599";
    static final String DF775_TAG = "775";
    static final String DF856_TAG = "856";
    static final Character EMPTY_IND = ' ';
    static final Character SUBFIELD_A = 'a';
    static final Character SUBFIELD_B = 'b';
    static final Character SUBFIELD_C = 'c';
    static final String BIBNUMBER = "(DK-810010)"; //TODO: changing til DK-800010

    public static final String[] TAGS = {DF245_TAG, "084", "100", DF599_TAG, "700", "710"};

    public AlmaClient(String url, String apikey) {
        this.apikey = apikey;
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        resource = client.resource(url);
    }

    /**
     * Retrieve a Bib record
     *
     * @param bibId The mmsId of the record to retrieve
     * @return The Bib object from Alma
     * @throws AlmaConnectionException Error message in case of Alma GET failure
     */
    public Bib getBibRecord(String bibId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s", bibId));
        return response == null ? null : response.getEntity(Bib.class);
    }

    /**
     * Update the contents of an existing Bib record in Alma
     *
     * @param record The Bib record to update. The values of the Bib record that should be updated, must have been
     *               changed before calling this method
     * @return The new updated Bib record
     * @throws AlmaConnectionException Error message in case of PUT failure
     */
    public Bib updateBibRecord(Bib record) throws AlmaConnectionException {
        return almaPUT(record);
    }

    /**
     * This sets whether the record should be published to Primo.
     * true means that the record will NOT be published. The subfield 'u' of datafield 096 will be set to:
     * "Kan ikke hjemlånes"
     *
     * @param bibId         The record Id of the record to suppress
     * @param suppressValue String value "true" means to suppress and "false" not to suppress
     * @return the Bib record
     * @throws AlmaConnectionException Error message in case of PUT failure
     * @throws MarcXmlException        Error message in case of MarcRecord handling error
     */
    public Bib setSuppressFromPublishing(String bibId, String suppressValue) throws AlmaConnectionException,
        MarcXmlException {
        Bib record = getBibRecord(bibId);
        record.setSuppressFromPublishing(suppressValue);
        if (suppressValue.equals("true")) {
            Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(record);
            MarcRecordHelper.addSubfield(marcRecord, "096", 'u', "Kan ikke hjemlånes");
            MarcRecordHelper.saveMarcRecordOnAlmaRecord(record, marcRecord);
        }
        return almaPUT(record);
    }

    /**
     * Create a new basic Bib record with only 'Title' set. The record should be updated {@link #updateBibRecord(Bib)}
     * with relevant values after creation, e.g. Leader, ControlFields and relevant DataFields
     *
     * @return The Alma Bib record with Title = "NewTitle"
     * @throws AlmaConnectionException Error message in case of POST failure
     * @throws MarcXmlException        Error message in case of MarcRecord handling error
     */
    public Bib createBibRecord() throws AlmaConnectionException, MarcXmlException {
        Bib bibRecord = new Bib();
        MarcRecordHelper.createMarcRecord(bibRecord);
        return almaPOST(bibRecord);
    }

    /**
     * Create a new Alma record for the digital object based on the analog. Some fields are copied, some are
     * created based on data from the analog report and some are new
     *
     * @param bibId   The mmsId of the analog record
     * @param pdfLink Link to the digitized book
     * @return The new digital record
     * @throws AlmaConnectionException Error message in case of POST failure. The creation of a new record failed
     * @throws MarcXmlException        Error message in case of MarcRecord handling error
     */
    public Bib createDigitalRecordFromAnalog(String bibId, String pdfLink) throws AlmaConnectionException,
        MarcXmlException {
        Bib analogRecord = getBibRecord(bibId);
        Bib digitalRecord = createBibRecord();
        MarcRecordHelper.createMarcRecord(digitalRecord);

        // Helper records to manipulate data in the Bib records
        Record anaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(analogRecord);
        Record digiMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(digitalRecord);

        ZoneId zoneId = ZoneId.of("Europe/Copenhagen");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        Month m = now.getMonth();
        int monthNumber = m.getValue();
        String currentMonth = String.format("%02d", monthNumber);

        // Get digitizationdate from controlfield 008 else set current year
        String controlField008 = MarcRecordHelper.getControlField(analogRecord, "008");
        String digiYear = controlField008 != null ? controlField008.substring(7, 10) : null;
        if (digiYear == null) {
            digiYear = Year.now().toString();
        }


        // Copy the fields without changes one-to-one
        //  TODO: Is TAGS list complete?
        MarcRecordHelper.getVariableField(analogRecord, digiMarcRecord, TAGS);

        // 035
        MarcRecordHelper.getVariableFields(analogRecord, digiMarcRecord, DF035_TAG);

        // Create new fields
        MarcRecordHelper.addDataField(digiMarcRecord, DF090_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A, "0");
        addSubfieldLocal(digiMarcRecord, DF090_TAG);

        String material = "Bog";
/*
        String formOfItem = controlField008 != null ? controlField008.substring(23) : null;
        if (formOfItem == null){
            material = "Tidsskrift";
        }
*/
        MarcRecordHelper.addDataField(digiMarcRecord, DF091_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A, material);
        addSubfieldLocal(digiMarcRecord, DF091_TAG);

        MarcRecordHelper.addDataField(digiMarcRecord, DF260_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_C, digiYear);

        // 500
        String dig = "Digitalisering " + digiYear + " af udgaven: ";
        String f260a = MarcRecordHelper.getSubfieldValue(analogRecord, DF260_TAG, SUBFIELD_A);
        String f260b = MarcRecordHelper.getSubfieldValue(analogRecord, DF260_TAG, SUBFIELD_B);
        String f260c = MarcRecordHelper.getSubfieldValue(analogRecord, DF260_TAG, SUBFIELD_C);
        String f300a = MarcRecordHelper.getSubfieldValue(analogRecord, DF300_TAG, SUBFIELD_A);
        String f300b = MarcRecordHelper.getSubfieldValue(analogRecord, DF300_TAG, SUBFIELD_B);
        MarcRecordHelper.addDataField(digiMarcRecord, DF500_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A,
            dig + f260a + f260b + f260c + f300a + f300b);
        // 909 The same as 500 above?

        // 500
        MarcRecordHelper.getVariableFields(analogRecord, digiMarcRecord, DF500_TAG);

        // 500
        String s = "Kontakt Spørg Biblioteket, hvis du har brug for at se den fysiske bog på Forskningslæsesalen";
        MarcRecordHelper.addDataField(digiMarcRecord, DF500_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A, s);

        // 500
        String ex = "Efter Det Kgl Biblioteks eksemplar: ";
        String f096a = MarcRecordHelper.getSubfieldValue(analogRecord, "096", SUBFIELD_A);
        MarcRecordHelper.addDataField(digiMarcRecord, DF500_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A,
            ex + f096a);

        // 599
        MarcRecordHelper.addDataField(digiMarcRecord, DF599_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_B,
            "Digi" + digiYear + currentMonth);
        addSubfieldLocal(digiMarcRecord, DF599_TAG);

        // 775
        String systemNumber = MarcRecordHelper.getSystemNumber(analogRecord, anaMarcRecord);
        String linkText = "Link mellem trykt og elektronisk udgave";
        MarcRecordHelper.addDataField(digiMarcRecord, DF775_TAG, EMPTY_IND, EMPTY_IND, SUBFIELD_A, systemNumber);
        MarcRecordHelper.addSubfield(digiMarcRecord, DF775_TAG, 't', linkText);

        // 856
        // TODO: how to get size
        String size = " MB";
        MarcRecordHelper.addDataField(digiMarcRecord, DF856_TAG, EMPTY_IND, EMPTY_IND, 'u', pdfLink);
        MarcRecordHelper.addSubfield(digiMarcRecord, DF856_TAG, 'z', size);

        // 901

        // 908
        String r096 = MarcRecordHelper.getSubfieldValue(analogRecord, "096", 'r');
        MarcRecordHelper.addDataField(digiMarcRecord, "908", EMPTY_IND, EMPTY_IND, SUBFIELD_A, r096);

        // 909

        // 997
        // TODO: awaiting answer from Mette Abildgaard
        // addSubfieldLocal(digiMarcRecord, "997");
        // 998
        // TODO: awaiting answer from Mette Abildgaard


        // Copy all the fields from the Marc Record to the new digital Alma record
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(digitalRecord, digiMarcRecord);
        return almaPOST(digitalRecord);
    }

    /**
     * Delete the specified Alma record
     *
     * @param mmsId The mmsId of the record to delete
     * @return true if success
     * @throws AlmaConnectionException Error message in case of DELETE failure
     */
    public boolean deleteBibRecord(String mmsId) throws AlmaConnectionException {
        String path = String.format("bibs/%s/", mmsId);
        WebResource.Builder builder = createBuilder(path);
        ClientResponse response = builder.delete(ClientResponse.class, path);
        if (response.getStatus() == 204) {
            log.info("Bib record (mmsId '{}') was deleted.", mmsId);
            return true;
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to delete Alma Bib record. " + errorMessage);
        }
    }

    public Holdings getBibHoldings(String bibId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s/holdings", bibId));
        return response == null ? null : response.getEntity(Holdings.class);
    }

    public Item createItem(String bibId, String holdingId, String barcode, String description, String pages,
                           String year) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(String.format("bibs/%s/holdings/%s/items", bibId, holdingId));

        Item item = new Item();
        ItemData itemData = new ItemData();
        itemData.setBarcode(barcode);
        itemData.setDescription(description);
        itemData.setPages(pages);
        itemData.setYearOfIssue(year);
        //TODO: set baseStatus
        item.setItemData(itemData);

        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("item"), Item.class,
            item));
        if (response.getStatus() == 200) {
            return response.getEntity(Item.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma item. " + errorMessage);
        }
    }

    public Item updateItem(Item item) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(
            String.format("bibs/%s/holdings/%s/items/%s", item.getBibData().getMmsId(),
                item.getHoldingData().getHoldingId(), item.getItemData().getPid()));

        ClientResponse response = builder.put(ClientResponse.class, new JAXBElement<>(new QName("item"), Item.class,
            item));
        if (response.getStatus() == 200) {
            return response.getEntity(Item.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to update Alma item. " + errorMessage);
        }
    }

    public User getUser(String userId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("users/%s", userId));
        return response == null ? null : response.getEntity(User.class);
    }

    public UserRequest getRequest(String userId, String requestId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("users/%s/requests/%s", userId, requestId));
        return response == null ? null : response.getEntity(UserRequest.class);
    }

    public UserResourceSharingRequest getResourceSharingRequest(String userId, String requestId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("users/%s/resource-sharing-requests/%s", userId, requestId));
        return response == null ? null : response.getEntity(UserResourceSharingRequest.class);
    }

    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     * @throws AlmaConnectionException if something went wrong
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, boolean notifyUser) throws AlmaConnectionException {
        return cancelRequest(userId, requestId, reasonCode, null, notifyUser);
    }

    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param note       Additional note for the user
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     * @throws AlmaConnectionException if something went wrong
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, String note, boolean notifyUser) throws AlmaConnectionException {
        WebResource.Builder builder;
        if (note != null) {
            builder = createBuilder(String.format("users/%s/requests/%s", userId, requestId),
                new QueryParam("reason", reasonCode), new QueryParam("note", note), new QueryParam("notify_user",
                    Boolean.toString(notifyUser)));
        } else {
            builder = createBuilder(String.format("users/%s/requests/%s", userId, requestId),
                new QueryParam("reason", reasonCode), new QueryParam("notify_user", Boolean.toString(notifyUser)));
        }
        ClientResponse response = builder.delete(ClientResponse.class);
        if (response.getStatus() == 204) {
            return true;
        }
        AlmaError almaError = getResponseError(response);
        if ("401694".equals(almaError.errorCode)) { // Request not found. Possible error codes can be found at the
            // Alma developer network
            return false;
        }

        throw new AlmaConnectionException("Failed to cancel Alma request. " + almaError.errorMessage);
    }

    /**
     * Create request for an item in alma
     *
     * @param userId             Id of the user
     * @param recordId           RecordId
     * @param holdingId          HoldingId
     * @param itemId             ItemId
     * @param pickupLocationCode A valid Alma pickupLocationCode
     * @param lastInterestDate   Last
     * @return response
     * @throws AlmaConnectionException if something went wrong
     */
    public UserRequest createRequest(String userId, String recordId, String holdingId, String itemId,
                                     String pickupLocationCode, XMLGregorianCalendar lastInterestDate) throws AlmaConnectionException {
        String path = String.format("bibs/%s/holdings/%s/items/%s/requests",
            recordId, holdingId, itemId);
        WebResource.Builder builder = createBuilder(path, new QueryParam("user_id", userId), new QueryParam(
            "user_id_type", "all_unique"));
        UserRequest userRequest = new UserRequest();
        userRequest.setRequestType(RequestTypes.HOLD);
        userRequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        userRequest.setPickupLocationLibrary(pickupLocationCode);
        if (lastInterestDate != null) {
            userRequest.setLastInterestDate(lastInterestDate);
        }
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("user_request"),
            UserRequest.class, userRequest));
        if (response.getStatus() == 200) {
            return response.getEntity(UserRequest.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma request. " + errorMessage);
        }
    }

    /**
     * Create request in alma
     *
     * @param request The fully populated request
     * @return The request created in Alma
     */
    public UserRequest createRequest(UserRequest request) throws AlmaConnectionException {
        String userId = request.getUserPrimaryId();
        String mmsId = request.getMmsId();
        String itemId = request.getItemId();
        String path = String.format("users/%s/requests", userId);
        WebResource.Builder builder;
        if (itemId != null) {
            builder = createBuilder(path, new QueryParam("user_id", userId), new QueryParam("user_id_type",
                "all_unique"), new QueryParam("mms_id", mmsId), new QueryParam("item_pid", itemId));
        } else {
            builder = createBuilder(path, new QueryParam("user_id", userId), new QueryParam("user_id_type",
                "all_unique"), new QueryParam("mms_id", mmsId));
        }
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("user_request"),
            UserRequest.class, request));
        if (response.getStatus() == 200) {
            return response.getEntity(UserRequest.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma request. " + errorMessage);
        }
    }

    public UserResourceSharingRequest createResourceSharingRequest(UserResourceSharingRequest request, String userId) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(String.format("users/%s/resource-sharing-requests", userId));
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName(
            "user_resource_sharing_request"), UserResourceSharingRequest.class, request));
        if (response.getStatus() == 200) {
            return response.getEntity(UserResourceSharingRequest.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma resource sharing request. " + errorMessage);
        }
    }

    public Item getItem(String bibId, String holdingId, String itemID) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s/holdings/%s/items/%s", bibId, holdingId, itemID));
        return response == null ? null : response.getEntity(Item.class);
    }

    /**
     * Get an Item by barcode
     *
     * @param barcode barcode of the Item
     * @return item
     * @throws AlmaConnectionException if something went wrong
     */
    public Item getItem(String barcode) throws AlmaConnectionException {
        ClientResponse response = get("items", new QueryParam("item_barcode", barcode));
        return response == null ? null : response.getEntity(Item.class);
    }

    public Items getItems(String bibId, String holdingId) throws AlmaConnectionException {
        int limit = 100;
        ClientResponse response = get(String.format("bibs/%s/holdings/%s/items", bibId, holdingId), new QueryParam(
            "limit", String.valueOf(limit)));
        Items items = response == null ? null : response.getEntity(Items.class);
        if (items != null && items.getItem().size() == limit) {
            log.warn("Retrieved max number of items ({}) for record '{}', holding '{}'. There might be more..", limit
                , bibId, holdingId);
        }
        return items;
    }

    public List<UserRequest> getItemRequests(String recordId, String holdingId, String itemId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s/holdings/%s/items/%s/requests", recordId, holdingId,
            itemId));
        return response == null ? new ArrayList<>() : response.getEntity(UserRequests.class).getUserRequest();
    }

    public CodeTable getCodeTable(String codeTableName) throws AlmaConnectionException {
        return getCodeTable(codeTableName, "da");
    }

    public CodeTable getCodeTable(String codeTableName, String lang) throws AlmaConnectionException {
        ClientResponse response = get(String.format("conf/code-tables/%s", codeTableName), new QueryParam("lang",
            lang));
        return response == null ? null : response.getEntity(CodeTable.class);
    }

    /**
     * Add a subfield with code '9' and value "LOCAL"
     *
     * @param marcRecord The marc record
     * @param tag        Subfield will be added to this datafield
     */
    private void addSubfieldLocal(Record marcRecord, String tag) {
        MarcRecordHelper.addSubfield(marcRecord, tag, '9', "LOCAL");
    }

    /**
     * Send PUT to Alma and get response
     *
     * @param record The alma record to manipulate
     * @return The response from Alma
     * @throws AlmaConnectionException If PUT operation failed
     */
    private Bib almaPUT(Bib record) throws AlmaConnectionException {
        String mmsId = record.getMmsId();
        WebResource.Builder builder = createBuilder(String.format("bibs/%s", mmsId));
        ClientResponse response = builder.put(ClientResponse.class, new JAXBElement<>(new QName("bib"), Bib.class,
            record));
        if (response.getStatus() == 200) {
            return response.getEntity(Bib.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            log.warn("Bib record '{}' was not updated: ", mmsId);
            throw new AlmaConnectionException("Failed to update Alma Bib record. " + errorMessage);
        }
    }

    /**
     * Send POST to Alma and get response
     *
     * @param bibRecord The alma record to create
     * @return The response from Alma
     * @throws AlmaConnectionException If POST operation failed
     */
    private Bib almaPOST(Bib bibRecord) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder("bibs");
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("bib"), Bib.class,
            bibRecord));
        if (response.getStatus() == 200) {
            Bib res = response.getEntity(Bib.class);
            log.info("New record created with mmsId: " + res.getMmsId());
            return res;
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma bibRecord. " + errorMessage);
        }
    }

    private ClientResponse get(String path, QueryParam... queryParams) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(path, queryParams);

        ClientResponse clientResponse = builder.get(ClientResponse.class);

        if (clientResponse.getStatus() == 200) {
            return clientResponse;
        } else {
            if (clientResponse.getStatus() == 400) { // Nothing is found
                return null;
            }
        }
        throw new AlmaConnectionException("Alma " + path + " GET request failed. " + getResponseError(clientResponse));
    }

    private AlmaError getResponseError(ClientResponse response) throws AlmaConnectionException {
        try (InputStream entityStream = response.getEntityInputStream()) {
            Document responseDocument = DOM.streamToDOM(entityStream);
            String errorMessage = DOM.selectString(responseDocument, "web_service_result/errorList/error/errorMessage");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Status: " + response.getStatus();
            }
            String errorCode = DOM.selectString(responseDocument, "web_service_result/errorList/error/errorCode");
            return new AlmaError(errorCode, errorMessage);
        } catch (IOException e) {
            throw new AlmaConnectionException("Failed to get error from Alma response", e);
        }
    }

    private WebResource.Builder createBuilder(String path, QueryParam... queryParams) {
        WebResource webResource = resource.path(path);
        for (QueryParam queryParam : queryParams) {
            webResource = webResource.queryParam(queryParam.key, queryParam.value);
        }
        return webResource
            .type(MediaType.APPLICATION_XML)
            .header("Authorization", "apikey " + apikey);
    }

    private static class AlmaError {
        String errorCode, errorMessage;

        public AlmaError(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    private static class QueryParam {
        private String key, value;

        public QueryParam(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}
