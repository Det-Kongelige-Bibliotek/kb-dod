package dk.kb.dod;

import dk.kb.alma.gen.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Dod {

    private static final Logger log = LoggerFactory.getLogger(Dod.class);

    public static void main(String[] args) throws Exception {
        String call = dodWorkFlow("115408052665");
        System.out.println("bibid is = "+ call);
    }

    public static String dodWorkFlow(String barcode) throws Exception{
        Properties dodpro = new Properties();
        dodpro.load(Dod.class.getClassLoader().getResourceAsStream("dod.properties"));

        String apikey = dodpro.getProperty("alma.apikey");
        String url = dodpro.getProperty("alma.url");
        AlmaClient almaclient = new AlmaClient(url, apikey,10000,30000,"da");
        Item myItem = almaclient.getItem(barcode);  // getRecord(bibId, url, apikey);
        return  myItem.getBibData().getMmsId();





    }

}
