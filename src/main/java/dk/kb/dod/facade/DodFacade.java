package dk.kb.dod.facade;

import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.General;
import dk.kb.alma.gen.Requests;
import dk.kb.alma.gen.User;
import dk.kb.dod.AlmaClient;
import dk.kb.dod.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class DodFacade {


    private Logger log = LoggerFactory.getLogger(DodFacade.class);

    private Logger STATUS_LOG = LoggerFactory.getLogger("STATUS");


    private final AlmaClient almaClient;
    private final Properties emailProperties;
    private final String almaEnvType;
    private final String almaHost;

    public DodFacade(AlmaClient almaClient, Properties emailProperties) {
        this.almaClient = almaClient;
        this.emailProperties = emailProperties;

        log.debug("Getting ALMA general info to determine alma host");
        General almaGeneral = almaClient.getLinkValue(almaClient.constructLink().path("/conf/general"), General.class);
        this.almaEnvType = almaGeneral.getEnvironmentType();
        try {
            this.almaHost = new URL(almaGeneral.getAlmaUrl()).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        log.debug("alma host = {}", this.almaHost);
        log.info("Initialized {}",getClass().getName());
    }

    public String dodWork(String barcode) {
        return barcode;
    }

    public Requests getRequest(String requestId) {
        return almaClient.getRequests(requestId);
    }

    public Bib getBib(String mmsID) {
        return almaClient.getBib(mmsID);
    }

    public User getUser(String userID) {
        return almaClient.getUser(userID);
    }


    public void sendMail(String user, String recipient, String title, String bookUrl) throws IOException {
        String text1 = emailProperties.getProperty("mail.text1");
        String text2 = emailProperties.getProperty("mail.text2");
        String text3 = emailProperties.getProperty("mail.text3");
        String bodyText = text1 + title + text2 + bookUrl + text3;

        String from = emailProperties.getProperty("mail.sender");
        String subject = "Bestilling gennemført";

        EmailSender.newInstance()
                   .to(recipient)
                   .from(from)
                   .subject(subject)
                   .bodyText(bodyText)
                   .send(emailProperties);
    }

    public String getAlmaEnvType() {
        return almaEnvType;
    }

    public String getAlmaHost() {
        return almaHost;
    }
}
