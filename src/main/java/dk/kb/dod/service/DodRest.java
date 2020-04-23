package dk.kb.dod.service;

import dk.kb.dod.facade.DodFacade;

import javax.ws.rs.*;

@Path("/services")
public class DodRest {


    private final DodFacade facade;


    public DodRest(DodFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("dod/{barcode}")
    public String dodWork(@PathParam("barcode") String barcode) {
        return facade.dodWork(barcode);
    }
}
