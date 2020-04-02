package dk.kb.dod;

import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.additional.Holdings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.Record;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

class AlmaClientTest {
    private static final String SANDBOX_APIKEY = "l8xx570d8eccc65b4fc3a8fbb512784181bd";

    @Ignore
    @Test
    public void testCreateAndDeleteBibRecord() throws AlmaConnectionException, MarcXmlException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
        Bib bib = almaClient.createBibRecord();
        assertNotNull(bib);
        assertTrue(almaClient.deleteBibRecord(bib.getMmsId()));
    }

    @Ignore
    @Test
    public void testUpdateBibRecord() throws AlmaConnectionException, MarcXmlException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
        String bibId =  "99123299347505763";
        Bib record = almaClient.getBibRecord(bibId);
        assertNotNull(record);

        String newTitle = "AnotherTitle";
        Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(record);
        MarcRecordHelper.setDataField(marcRecord, "245", 'a', newTitle );
//        assertTrue(MarcRecordHelper.setTitle(marcRecord, newTitle));


        MarcRecordHelper.saveMarcRecordOnAlmaRecord(record, marcRecord);
        Bib updatedRecord = almaClient.updateBibRecord(record);
        assertEquals(newTitle, updatedRecord.getTitle());
    }

/*    @Ignore
        @Test
        public void createItem() throws AlmaConnectionException {
            AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
            long barcode = (long) (Math.random() * 999999999999L);
            Item item = almaClient.createItem("99123290311205763", "222104136990005763", String.valueOf(barcode), "test item", "1", "2000");
    //                                              99120789920105763     222104137000005763, 222104136990005763
            String title = item.getBibData().getTitle();
            String itemBarcode = item.getItemData().getBarcode();
            System.out.println("Created new item with barcode: " + itemBarcode + " and title: " + title);
        }*/
//    @Ignore
//    @Test
//    public void updateItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99120789920105763", "221199059350005763", "231615214960005763");
//
//        Assert.assertNotNull(item);
//
//        String newBarcode = String.valueOf((long) (Math.random() * 999999999999L));
//        item.getItemData().setBarcode(String.valueOf(newBarcode));
//        Item updatedItem = almaClient.updateItem(item);
//
//        Assert.assertEquals(newBarcode, updatedItem.getItemData().getBarcode());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99123290311205763", "221199059350005763", "231615214960005763");
//
//        Assert.assertEquals("test item", item. getItemData().getDescription());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItemByBarcode() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("v22333");
//
//        Assert.assertEquals("Created via Elba.", item.getItemData().getDescription());
//    }
/*

    @Ignore
    @Test
    public void testGetItems() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        Items items = almaClient.getItems("99120661858005763", "221157462480005763");

        assertTrue(items.getItem().size() >= 2);
    }

*/
    @Ignore
    @Test
    public void testGetHoldings() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        Holdings holdings = almaClient.getBibHoldings("99123290311205763");

        assertNotNull(holdings);
        assertTrue(holdings.getHolding().size() >= 3);
    }

    @Ignore
    @Test
    public void testGetBibRecord() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        Bib bib = almaClient.getBibRecord("99123290311205763");

        Assert.assertEquals("99123290311205763", bib.getMmsId());
    }

    @Ignore
    @Test
    public void testGetBibRecordWithNonExistingRecord() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        Bib bib = almaClient.getBibRecord("fail");

        assertNull(bib);
    }
//
    @Ignore
    @Test
    public void testGetBibRecordWithFailingPath() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/fail/", SANDBOX_APIKEY);

        assertThrows(AlmaConnectionException.class, () -> almaClient.getBibRecord("fail"));
    }

    @Ignore
    @Test
    public void testGetUser() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        User user = almaClient.getUser("dahe");

        Assert.assertEquals("Dan", user.getFirstName().trim());
    }

    @Ignore
    @Test
    public void testGetUserForNonexistingUser() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        User user = almaClient.getUser("nonexistinguser");

        assertNull(user);
    }

    @Ignore
    @Test
    public void testCancelRequestWithNonexistingRequest() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        boolean success = almaClient.cancelRequest("dahe", "999999999999999", "PatronNotInterested", true);

        assertFalse("Cancellation should fail.", success);
    }

//    @Ignore
//    @Test
//    public void testCreateRequestAndCancelRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = almaClient.createRequest("thl", "99120747423805763", "221185306080005763", "231185306070005763", "SBL", null);
//
//        assertTrue(request.getTitle().startsWith("Ja!"));
//
//        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
//
//        assertTrue(success);
//    }
//
//    @Ignore
//    @Test
//    public void testCreateAndCancelItemRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = new UserRequest();
//        request.setUserPrimaryId("thl");
//        request.setRequestType(RequestTypes.HOLD);
//        request.setMmsId("99120402557905763");
//        request.setItemId("231073573770005763");
//        request.setPickupLocationType(PickupLocationTypes.LIBRARY);
//        request.setPickupLocationLibrary("UMOES");
//
//        request = almaClient.createRequest(request);
//
//        assertTrue(request.getTitle().startsWith("Eine warme Kartoffel ist ein warmes Bett"));
//
//        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
//
//        assertTrue(success);
//    }
//
//
//    @Ignore
//    @Test
//    public void testCreateAndCancelResourceSharingRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserResourceSharingRequest request = createResourceSharingRequest();
//        UserResourceSharingRequest createdRequest = almaClient.createResourceSharingRequest(request, "thl");
//
//        Assert.assertEquals("Integration testtt", createdRequest.getTitle());
//
//        createdRequest = almaClient.getResourceSharingRequest("thl", createdRequest.getRequestId());
//        String userRequestLink = createdRequest.getUserRequest().getLink();
//        Assert.assertNotNull(userRequestLink);
//        String userRequestId = userRequestLink.substring(userRequestLink.lastIndexOf("/")+1);
//        assertFalse(userRequestId.isEmpty());
//
//        boolean cancelled = almaClient.cancelRequest("thl", userRequestId, "PatronNotInterested", false);
//
//        assertTrue(cancelled);
//    }
//
//    private UserResourceSharingRequest createResourceSharingRequest() {
//        UserResourceSharingRequest request = new UserResourceSharingRequest();
//        request.setTitle("Integration testtt");
//        UserResourceSharingRequest.Format format = new UserResourceSharingRequest.Format();
//        format.setValue("PHYSICAL");
//        request.setFormat(format);
//        UserResourceSharingRequest.CitationType citationType = new UserResourceSharingRequest.CitationType();
//        citationType.setValue("BK");
//        request.setCitationType(citationType);
//        request.setAgreeToCopyrightTerms(true);
//        UserResourceSharingRequest.PickupLocation pickupLocation = new UserResourceSharingRequest.PickupLocation();
//        pickupLocation.setValue("RRRUC");
//        request.setPickupLocation(pickupLocation);
//        return request;
//    }
//
//    @Ignore
//    @Test
//    public void testGetRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = almaClient.getRequest("thl", "12301266660005763");
//
//        Assert.assertEquals("The hitchhiker's guide to the galaxy", request.getTitle());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItemRequests() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        List<UserRequest> itemRequests = almaClient.getItemRequests("99120962982505763", "221255954600005763", "231255954590005763");
//
//        assertTrue(itemRequests.size() > 0);
//
//        assertTrue("There should be a request from user 'thl'", itemRequests.stream().anyMatch(request -> request.getUserPrimaryId().equals("thl")));
//    }
//
//    @Ignore
//    @Test
//    public void testGetResourceSharingRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserResourceSharingRequest request = almaClient.getResourceSharingRequest("thl", "12482165450005763");
//
//        Assert.assertEquals("testtest", request.getTitle());
//    }
//
/*
    @Ignore
    @Test
    public void testGetCodeTable() throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);

        CodeTable requestCancellationReasons = almaClient.getCodeTable("RequestCancellationReasons");
        Rows rows = requestCancellationReasons.getRows();
        Assert.assertNotNull(rows);
        assertTrue(rows.getRow().size() > 0);
    }
*/
}
