package dk.kb.dod;

import dk.kb.alma.gen.Bib;
//import dk.statsbiblioteket.elba.elba.facade.ElbaFacade;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.io.IOUtils;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MarcRecordHelper {
    /**
     * Helper class for editing marcXml on an Alma record
     */

    private static final Logger log = LoggerFactory.getLogger(MarcRecordHelper.class);

    private static final String TITLE_TAG = "245";
    private static final char TITLE_CODE = 'a';
    private static final String AUTHOR_TAG = "100";
    private static final char AUTHOR_CODE = 'a';

    public static void createMarcRecord(Bib bibRecord) throws MarcXmlException {
        MarcFactory marcFactory = MarcFactory.newInstance();
        Record marcRecord = marcFactory.newRecord();
        // Add minimum contents for creating a new Bib record (Title)
        DataField dataField = marcFactory.newDataField(TITLE_TAG,'1','0');
        dataField.addSubfield(marcFactory.newSubfield(TITLE_CODE, "NewBook"));
        marcRecord.addVariableField(dataField);
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(bibRecord, marcRecord);
    }

    public static Record getMarcRecordFromAlmaRecord(Bib almaRecord) throws MarcXmlException {
        if(almaRecord.getAny().size() != 4){
            throw new MarcXmlException("4 marcXml objects expected, but " + almaRecord.getAny().size() +
                " was found on Alma record with id: " + almaRecord.getMmsId());
        }
        Node marcXmlNode = (Node) almaRecord.getAny().get(3);
        try (InputStream marcXmlStream = IOUtils.toInputStream(DOM.domToString(marcXmlNode, false))) {
            MarcXmlReader marcXmlReader = new MarcXmlReader(marcXmlStream);
            Record marcRecord;
            if (marcXmlReader.hasNext()) {
                marcRecord = marcXmlReader.next();
            } else {
                throw new MarcXmlException("No marc record found in marcXml on Alma record with id: " + almaRecord.getMmsId());
            }
            if(marcXmlReader.hasNext()){
                throw new MarcXmlException("Multiple marc records found in marcXml on Alma record with id: " +
                    almaRecord.getMmsId());
            }
            return marcRecord;
        } catch (TransformerException | IOException e) {
            throw new MarcXmlException("Failed to read marcXml from Alma record with id: " + almaRecord.getMmsId(), e);
        }
    }

    public static void saveMarcRecordOnAlmaRecord(Bib almaRecord, Record marcRecord) throws MarcXmlException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MarcXmlWriter marcXmlWriter = new MarcXmlWriter(out);
            marcXmlWriter.write(marcRecord);
            marcXmlWriter.close();
            String marcXmlString = out.toString();
            Element marcElement = DOM.stringToDOM(marcXmlString, false).getDocumentElement();

            almaRecord.getAny().clear();
            almaRecord.getAny().add(marcElement);
        } catch (IOException e) {
            throw new MarcXmlException("Failed to save marc record on Alma record");
        }
    }

    /**
     * Sets the title on the marc record. Assumes that the field already exists
     * @return true if the title is successfully set
     * false if the record is missing the field
     */
    public static boolean setTitle(Record marcRecord, String title) {
        return setDataField(marcRecord, TITLE_TAG, TITLE_CODE, title);
    }

    /**
     * Sets the author on the marc record. Assumes that the field already exists
     * @return true if the author is successfully set.
     * false if the record is missing the field
     */
    public static boolean setAuthor(Record marcRecord, String author) {
        return setDataField(marcRecord, AUTHOR_TAG, AUTHOR_CODE, author);
    }

    public static boolean setDataField(Record marcRecord, String dataFieldTag, char subFieldCode, String subfieldValue) {
        DataField field = (DataField) marcRecord.getVariableField(dataFieldTag);
        if(field == null){
            return false;
        }
        Subfield subfield = field.getSubfield(subFieldCode);
        if(subfield == null){
            return false;
        }
        field.getSubfields().get(0).setData(subfieldValue);
        return true;
    }


//    /**
//     * Extract the periodical type from an alma Bib record
//     * @param almaRecord
//     * @return the periodical type. The default is JOURNAL if the type cannot otherwise be determined
//     */
//
//        public static ElbaFacade.PeriodicalType getPeriodicalType(Bib almaRecord) {
//            ElbaFacade.PeriodicalType periodicalType = ElbaFacade.PeriodicalType.JOURNAL; //default
//            try {
//                Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
//                final char typeChar = marcRecord.getVariableField("008").toString().charAt(25);
//                if (typeChar == 'n') {
//                    periodicalType = ElbaFacade.PeriodicalType.NEWS;
//                } else if (typeChar == 'p')  {
//                    periodicalType = ElbaFacade.PeriodicalType.JOURNAL;
//                } else {
//                    log.warn("Didn't find periodical type in {} for {}, assuming Journal.", marcRecord.getVariableField("008"), almaRecord.getMmsId());
//                    periodicalType = ElbaFacade.PeriodicalType.JOURNAL;
//                }
//            } catch (MarcXmlException e) {
//                log.warn("Could not parse marc record {} in Bib {}.", almaRecord.getAny().stream().findFirst().toString(), almaRecord.getMmsId());
//            }
//            return periodicalType;
//        }

}

