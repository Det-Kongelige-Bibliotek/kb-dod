package dk.kb.dod.service;

import javax.ws.rs.*;

@Path("/")
public class DodRest {
    @GET
    @Path("dod/{barcode}")

    public String dodWork(@PathParam("barcode") String barcode) {
        System.out.println("called!");
    	return barcode;
    }
}
