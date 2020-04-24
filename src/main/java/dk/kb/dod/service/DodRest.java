package dk.kb.dod.service;

import dk.kb.alma.gen.Item;
import dk.kb.dod.AlmaClient;
import dk.kb.dod.facade.DodFacade;

import javax.ws.rs.*;
import java.io.IOException;
import java.util.Properties;

@Path("/services")
public class DodRest {


    private final DodFacade facade;


    public DodRest(DodFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("dod/{barcode}")
    public String dodWork(@PathParam("barcode") String barcode) throws IOException {
        Properties dodpro = new Properties();
        dodpro.load(DodRest.class.getClassLoader().getResourceAsStream("dod.properties"));

        String apikey = dodpro.getProperty("alma.apikey");
        String url = dodpro.getProperty("alma.url");
        AlmaClient almaclient = new AlmaClient(url, apikey,10000,30000,"da");
        Item myItem = almaclient.getItem(barcode);

        // createRecord(myItem.getBibData().getMmsId(), getUrl(barcode);


        return facade.dodWork(barcode);
    }
}
