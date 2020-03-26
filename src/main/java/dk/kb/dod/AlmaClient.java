package dk.kb.dod;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import dk.kb.alma.gen.*;
import dk.kb.alma.gen.additional.Holdings;
import dk.statsbiblioteket.util.xml.DOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class AlmaClient {
    private static final Logger log = LoggerFactory.getLogger(AlmaClient.class);
    private final WebResource resource;
    private String apikey;

    public AlmaClient(String url, String apikey) {
        this.apikey = apikey;
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        resource = client.resource(url);
    }


    public Holdings getBibHoldings(String bibId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s/holdings", bibId));
        return response == null ? null : response.getEntity(Holdings.class);
    }

    public Bib getBibRecord(String bibId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s", bibId));
        return response == null ? null : response.getEntity(Bib.class);
    }

    public Bib updateBibRecord(Bib record) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(String.format("bibs/%s", record.getMmsId()));
        ClientResponse response = builder.put(ClientResponse.class, new JAXBElement<>(new QName("bib"), Bib.class, record));
        if (response.getStatus() == 200) {
            return response.getEntity(Bib.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to update Alma Bib record. " + errorMessage);
        }
    }

    public Bib createBibRecord() throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder("bibs");
        Bib record = new Bib();

//        record.setMmsId("");  //            sæt values eller sker det automatisk ved create??
//        record.setRecordFormat("marc21");
//        record.setSuppressFromPublishing("false");
//        Bib.CatalogingLevel catalogingLevel = new Bib.CatalogingLevel();
//        catalogingLevel.setDesc("Default Level");
//        catalogingLevel.setValue("00");
//        record.setCatalogingLevel(catalogingLevel);
//        Bib.LinkedRecordId linkedRecordId = new Bib.LinkedRecordId();
//        linkedRecordId.setType("");
//        linkedRecordId.setValue("");
//        record.setLinkedRecordId(linkedRecordId);

        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("bib"), Bib.class, record));
        if (response.getStatus() == 200){
            return response.getEntity(Bib.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma record. " + errorMessage);
        }
    }

    public Item createItem(String bibId, String holdingId, String barcode, String description, String pages, String year) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(String.format("bibs/%s/holdings/%s/items", bibId, holdingId));

        Item item = new Item();
        ItemData itemData = new ItemData();
        itemData.setBarcode(barcode);
        itemData.setDescription(description);
        itemData.setPages(pages);
        itemData.setYearOfIssue(year);
        //TODO: set baseStatus
        item.setItemData(itemData);

        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("item"), Item.class, item));
        if (response.getStatus() == 200) {
            return response.getEntity(Item.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma item. " + errorMessage);
        }
    }

    public Item updateItem(Item item) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(
                String.format( "bibs/%s/holdings/%s/items/%s", item.getBibData().getMmsId(),
                    item.getHoldingData().getHoldingId(), item.getItemData().getPid()));

        ClientResponse response = builder.put(ClientResponse.class, new JAXBElement<>(new QName("item"), Item.class, item));
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
     * @param userId Id of the user with the request
     * @param requestId The request id
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
     * @param userId Id of the user with the request
     * @param requestId The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param note Additional note for the user
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
        if("401694".equals(almaError.errorCode)){ // Request not found. Possible error codes can be found at the Alma developer network
            return false;
        }

        throw new AlmaConnectionException("Failed to cancel Alma request. " + almaError.errorMessage);
    }

    /**
     * Create request for an item in alma
     * @param userId Id of the user
     * @param recordId RecordId
     * @param holdingId HoldingId
     * @param itemId ItemId
     * @param pickupLocationCode A valid Alma pickupLocationCode
     * @param lastInterestDate Last
     * @return response
     * @throws AlmaConnectionException if something went wrong
     */
    public UserRequest createRequest(String userId, String recordId, String holdingId, String itemId, String pickupLocationCode, XMLGregorianCalendar lastInterestDate) throws AlmaConnectionException {
        String path = String.format("bibs/%s/holdings/%s/items/%s/requests",
                recordId, holdingId, itemId);
        WebResource.Builder builder = createBuilder(path, new QueryParam("user_id", userId), new QueryParam(
            "user_id_type", "all_unique"));
        UserRequest userRequest = new UserRequest();
        userRequest.setRequestType(RequestTypes.HOLD);
        userRequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        userRequest.setPickupLocationLibrary(pickupLocationCode);
        if(lastInterestDate != null){
            userRequest.setLastInterestDate(lastInterestDate);
        }
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("user_request"), UserRequest.class, userRequest));
        if (response.getStatus() == 200) {
            return response.getEntity(UserRequest.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma request. " + errorMessage);
        }
    }

    /**
     * Create request in alma
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
            builder = createBuilder(path, new QueryParam("user_id", userId), new QueryParam("user_id_type", "all_unique"), new QueryParam("mms_id", mmsId));
        }
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("user_request"), UserRequest.class, request));
        if (response.getStatus() == 200) {
            return response.getEntity(UserRequest.class);
        } else {
            String errorMessage = getResponseError(response).errorMessage;
            throw new AlmaConnectionException("Failed to create Alma request. " + errorMessage);
        }
    }

    public UserResourceSharingRequest createResourceSharingRequest(UserResourceSharingRequest request, String userId) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(String.format("users/%s/resource-sharing-requests", userId));
        ClientResponse response = builder.post(ClientResponse.class, new JAXBElement<>(new QName("user_resource_sharing_request"), UserResourceSharingRequest.class, request));
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
        ClientResponse response = get(String.format("bibs/%s/holdings/%s/items", bibId, holdingId), new QueryParam("limit", String.valueOf(limit)));
        Items items = response == null ? null : response.getEntity(Items.class);
        if(items != null && items.getItem().size() == limit){
            log.warn("Retrieved max number of items ({}) for record '{}', holding '{}'. There might be more..", limit, bibId, holdingId);
        }
        return items;
    }

    public List<UserRequest> getItemRequests(String recordId, String holdingId, String itemId) throws AlmaConnectionException {
        ClientResponse response = get(String.format("bibs/%s/holdings/%s/items/%s/requests", recordId, holdingId, itemId));
        return response == null ? new ArrayList<>() : response.getEntity(UserRequests.class).getUserRequest();
    }

    public CodeTable getCodeTable(String codeTableName) throws AlmaConnectionException {
        return getCodeTable(codeTableName,"da");
    }

    public CodeTable getCodeTable(String codeTableName,String lang) throws AlmaConnectionException {
        ClientResponse response = get(String.format("conf/code-tables/%s", codeTableName), new QueryParam("lang", lang));
        return response == null ? null : response.getEntity(CodeTable.class);
    }

    private ClientResponse get(String path, QueryParam ... queryParams) throws AlmaConnectionException {
        WebResource.Builder builder = createBuilder(path, queryParams);

        ClientResponse clientResponse = builder.get(ClientResponse.class);

        if(clientResponse.getStatus() == 200){
            return clientResponse;
        } else {
            if(clientResponse.getStatus() == 400){ // Nothing is found
                return null;
            }
        }
        throw new AlmaConnectionException("Alma " + path + " GET request failed. " + getResponseError(clientResponse));
    }

    private AlmaError getResponseError(ClientResponse response) throws AlmaConnectionException {
        try (InputStream entityStream = response.getEntityInputStream()) {
            Document responseDocument = DOM.streamToDOM(entityStream);
            String errorMessage = DOM.selectString(responseDocument, "web_service_result/errorList/error/errorMessage");
            if(errorMessage == null || errorMessage.isEmpty()){
                errorMessage = "Status: " + response.getStatus();
            }
            String errorCode = DOM.selectString(responseDocument, "web_service_result/errorList/error/errorCode");
            return new AlmaError(errorCode, errorMessage);
        } catch (IOException e) {
            throw new AlmaConnectionException("Failed to get error from Alma response", e);
        }
    }

    private WebResource.Builder createBuilder(String path, QueryParam ... queryParams) {
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
