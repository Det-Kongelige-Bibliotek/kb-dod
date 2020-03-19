package dk.kb.dod;

import dk.kb.alma.gen.Bib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRecord {
    private static final Logger log = LoggerFactory.getLogger(AlmaClient.class);
    private static String apikey = "l8xx570d8eccc65b4fc3a8fbb512784181bd";
    private static  String url = "https://api-eu.hosted.exlibrisgroup.com/almaws/v1/";
    public static String bibId = "99123244163205763";
    public static Bib bib = new Bib();

    public static void main(String[] args) throws Exception {
        bib = getRecord(bibId, url, apikey);
        System.out.println("bibid is = "+ bib.getMmsId());
    }

    public static Bib getRecord(String bibId, String url, String apikey) throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient(url, apikey);
        bib = almaClient.getBibRecord(bibId);
        return bib;
    }
}
