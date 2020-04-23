package dk.kb.dod;

import dk.kb.alma.gen.User;


import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Session;
import javax.mail.MessagingException;
import java.util.Properties;
import javax.mail.Transport;

public class SendMail {
    //public static User bibUser = new User();
    //public static String userId = "88188998";
    //public static String userId = "kck";
    //public static String userId = "88229943";

    public static void sendMail (String userId, String email, String title, String  bookUrl) {
        Properties dodpro = new Properties();
        String text1 = dodpro.getProperty("mail.text1");
        String text2 = dodpro.getProperty("mail.text2");
        String text3 = dodpro.getProperty("mail.text3");
        String mailbody = text1 + title + text2 + bookUrl + text3;
        /*String text = "K\u00E6re,\n" +
            "\n" +
            "Den gratis digitale udgave af\n"+
            title +" \n\n"+
            "som du har bestilt, er nu tilg\u00E6ngelig.\n" +
            "Filen kan hentes her:\n" +
            bookUrl + "\n" +
            "\n" +
            "Med venlig hilsen\n" +
            "\n" +
            "Det Kongelige Bibliotek\n" +
            "EPUB@kb.dk\n";

         */
        // email ID of Rec432ipient.
        String recipient = email; //"nkh@kb.dk";  //bibUser.getContactInfo().getEmails().getEmail().get(0).getEmailAddress());

        // email ID of  Sender.
        String sender = dodpro.getProperty("mail.sender");  //"nkh@kb.dk";

        // using host as localhost
        String host = "localhost";

        // Getting system properties
        Properties properties = System.getProperties();

        // Setting up mail server
        properties.setProperty("mail.smtp.host", host);

        // creating session object to get properties
        Session session = Session.getDefaultInstance(properties);

        try
        {
            // MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From Field: adding senders email to from field.
            message.setFrom(new InternetAddress(sender));

            // Set To Field: adding recipient's email to from field.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            // Set Subject: subject of the email
            message.setSubject("Bestilling gennemf\u00F8rt");

            // set body of the email.
            message.setText(mailbody);

            // Send email.
            Transport.send(message);
            System.out.println("Mail successfully sent");
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
    }

}


