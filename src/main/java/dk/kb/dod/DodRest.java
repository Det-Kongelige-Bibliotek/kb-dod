package dk.kb.dod;

import javax.ws.rs.*;

@Path("/")
public class DodRest {
    @GET
    @Path("dod/{barcode}")

    public String dodWork(@PathParam("barcode") String barcode) {
        return barcode;
    }
}
