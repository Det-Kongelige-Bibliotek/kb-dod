package dk.kb.dod;

import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRecord {
    private static final Logger log = LoggerFactory.getLogger(AlmaClient.class);
    private static String apikey = "l8xx570d8eccc65b4fc3a8fbb512784181bd";
    private static  String url = "https://api-eu.hosted.exlibrisgroup.com/almaws/v1/";
    public static String bibId = "99123244163205763";
    public static Bib bib = new Bib();

    public static void main(String[] args) throws Exception {
        AlmaClient almaClient = new AlmaClient(url, apikey,10000,30000,"da");
        //bib = almaClient.getBibRecord(bibId);
        Item myItem = almaClient.getItem("115408052665");  // getRecord(bibId, url, apikey);
        System.out.println("bibid is = "+ myItem.getBibData().getMmsId());
        //System.out.println("bibid is = "+ bib.getMmsId());
    }

    public static String calling () throws Exception {
        bib = getRecord(bibId, url, apikey);
        //System.out.println("bibid is = "+ bib.getMmsId());
        return bib.getMmsId();
    }

    public static Bib getRecord(String bibId, String url, String apikey) throws AlmaConnectionException {
        AlmaClient almaClient = new AlmaClient(url, apikey, 10000,30000,"da");
        bib = almaClient.getBib(bibId);
        return bib;
    }
}
