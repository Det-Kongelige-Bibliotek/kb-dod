package dk.kb.dod;

import dk.kb.alma.gen.Bib;
//import dk.statsbiblioteket.elba.elba.facade.ElbaFacade;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.io.IOUtils;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MarcRecordHelper {
    /**
     * Helper class for editing marcXml on an Alma record
     */

    private static final Logger log = LoggerFactory.getLogger(MarcRecordHelper.class);

    private static final String TITLE_TAG = "245";
    private static final char TITLE_CODE = 'a';
    private static final String AUTHOR_TAG = "100";
    private static final char AUTHOR_CODE = 'a';

    /**
     * Create a title field when creating a new Bib record
     * @param almaRecord The new Bib record that gets the title
     * @throws MarcXmlException Exception in case of Marc handling error
     */
    public static void createMarcRecord(Bib almaRecord) throws MarcXmlException {
        try {
            MarcFactory marcFactory = MarcFactory.newInstance();
            Record marcRecord = marcFactory.newRecord();
            // Add minimum contents for creating a new Bib record (i.e. the title)
            DataField dataField = marcFactory.newDataField(TITLE_TAG,'1','0');
            dataField.addSubfield(marcFactory.newSubfield(TITLE_CODE, "NewTitle"));
            marcRecord.addVariableField(dataField);
            MarcRecordHelper.saveMarcRecordOnAlmaRecord(almaRecord, marcRecord);
        } catch (MarcXmlException e) {
            log.warn("Could not create marc record {} for Bib {}.", almaRecord.getAny().stream().findFirst().toString(), almaRecord.getMmsId());
            throw new MarcXmlException("Failed to create new record");
        }
    }

    public static Record getMarcRecordFromAlmaRecord(Bib almaRecord) throws MarcXmlException {
        Node marcXmlNode;
        int anySize = almaRecord.getAny().size();
        if(anySize == 4){
            marcXmlNode = (Node) almaRecord.getAny().get(3);
        } else if (anySize == 1){
            marcXmlNode = (Node) almaRecord.getAny().get(0);
        } else {
            throw new MarcXmlException("Wrong number of marcXml objects:  " + almaRecord.getAny().size() +
                " was found on Alma record with id: " + almaRecord.getMmsId());
        }

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

    /**
     * Update contents for an existing data field on a Marc record
     * @param marcRecord The Marc record
     * @param dataFieldTag tag, e.g. "100" (Author)
     * @param subFieldCode code (E.g. 'a')
     * @param subfieldValue value (E.g. "Andersen, H.C.")
     * @return false if the field is not present otherwise true
     */

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

    /**
     * Add a new @dataField with one subfield to a Marc record
     *
     * @param marcRecord The record to add new datafield to
     * @param dataFieldTag The tag to add (E.g. "100", "500")
     * @param dataFieldInd1 (E.g. '1')
     * @param dataFieldInd2 (E.g. '0' )
     * @param subfieldCode (E.g. 'a')
     * @param subfieldValue The text value of the subfield
     */
    public static void addDataField(Record marcRecord, String dataFieldTag, char dataFieldInd1, char dataFieldInd2,
                                    char subfieldCode, String subfieldValue) {

        MarcFactory marcFactory = MarcFactory.newInstance();
//            Record marcRecord = getMarcRecordFromAlmaRecord(almaRecord);
        DataField dataField = marcFactory.newDataField(dataFieldTag, dataFieldInd1, dataFieldInd2);
        dataField.addSubfield(marcFactory.newSubfield(subfieldCode, subfieldValue));
        marcRecord.addVariableField(dataField);

//            return marcRecord;

    }

    /**
     * Add a new @dataField with more subfields to a Marc record
     *
     * @param marcRecord The record to add new datafield to
     * @param dataFieldTag The tag to add (E.g. "100", "500")
     * @param dataFieldInd1 (E.g. '1')
     * @param dataFieldInd2 (E.g. ' ' )
     * @param subFields a list of the subfields to add
     */

    public static void addDataField(Record marcRecord, String dataFieldTag, char dataFieldInd1, char dataFieldInd2,
                                    List<Subfield> subFields ) {

        MarcFactory marcFactory = MarcFactory.newInstance();
//            Record marcRecord = getMarcRecordFromAlmaRecord(almaRecord);
        DataField dataField = marcFactory.newDataField(dataFieldTag, dataFieldInd1, dataFieldInd2);
        for (Subfield subfield : subFields) {
            char code = subfield.getCode();
            String data = subfield.getData();
            dataField.addSubfield(marcFactory.newSubfield(code, data));
        }
        marcRecord.addVariableField(dataField);
//            MarcRecordHelper.saveMarcRecordOnAlmaRecord(almaRecord, marcRecord);
//        return marcRecord;
    }

    /**
     * Add a subfield to an existing field
     * @param marcRecord The marc record to add the subfield to
     * @param dataFieldTag The tag to add the subfield to, e.g. "100"
     * @param subfieldCode The code of the subfield, e.g. 'b'
     * @param subfieldValue The data of the subfield e.g. "Some new data"
     */
    public static void addSubfield(Record marcRecord, String dataFieldTag, char subfieldCode, String subfieldValue) {
        MarcFactory marcFactory = MarcFactory.newInstance();
//            Record marcRecord = getMarcRecordFromAlmaRecord(almaRecord);
        DataField dataField = (DataField) marcRecord.getVariableField(dataFieldTag);
        dataField.addSubfield(marcFactory.newSubfield(subfieldCode, subfieldValue));
        marcRecord.addVariableField(dataField);
//            MarcRecordHelper.saveMarcRecordOnAlmaRecord(almaRecord, marcRecord);
//        return marcRecord;
    }

    public static DataField getDataField(Bib almaRecord, String tag ) throws MarcXmlException {
        try {
            Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
            return (DataField)marcRecord.getVariableField(tag);
        } catch (MarcXmlException e) {
            log.info("DataField {} was not found", tag);
            return null;
        }
    }
    public static Subfield getSubfieldValue(Bib almaRecord, String tag , Character subfield) throws MarcXmlException {
        Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
        try {
            DataField df = (DataField)marcRecord.getVariableField(tag);
            return df.getSubfield(subfield);
        } catch (Exception e) {
            log.info("DataField {} was not found", tag);
            return null;
        }
    }
    public static DataField getDataField(Record marcRecord, String tag ) throws MarcXmlException {
        try {
            return (DataField)marcRecord.getVariableField(tag);
        } catch (Exception e) {
            log.info("DataField {} was not found", tag);
            return null;
        }
    }

    /**
     * Get one (the first if more exist) datafield with the specified tag
     * @param almaRecord The (Alma) record to retrieve data from
     * @param marcRecord The (Marc) record to copy data to
     * @param tag The data tag to copy
     * @throws MarcXmlException Exception in case of Marc handling error
     */
    public static void getVariableField(Bib almaRecord, Record marcRecord, String tag) throws MarcXmlException {
        try {
            Record almaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
            marcRecord.addVariableField(almaMarcRecord.getVariableField(tag));
        } catch (MarcXmlException e) {
            log.info("DataField {} was not found", tag);
        }

    }

    public static void getVariableField(Bib almaRecord, Record marcRecord, String[] tags) throws MarcXmlException {

        Record almaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
        for (String tag : tags) {
            try {
                marcRecord.addVariableField(almaMarcRecord.getVariableField(tag));
            } catch (Exception e) {
                log.info("DataField {} was not found", tag);
            }
        }
    }

    /**
     * Get all occurrences with the specified tag
     * @param almaRecord The (Alma) record to retrieve data from
     * @param marcRecord The (Marc) record to copy data to
     * @param tag The tag to copy from
     * @throws MarcXmlException Exception in case of Marc handling error
     */
    public static void getVariableFields(Bib almaRecord, Record marcRecord, String tag) throws MarcXmlException {
        Record almaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
        List<VariableField> variableFields = almaMarcRecord.getVariableFields(tag);
        for (VariableField vf : variableFields) {
            try {
                marcRecord.addVariableField(vf);
            }catch (Exception e){
                log.info("DataField {} was not found", tag);
            }
        }
    }

    public static Subfield getSystemNumber(Bib almaRecord, Record marcRecord) throws MarcXmlException {
        String tag = "035";
        String bibNumber = "(DK-810010)";
        Record almaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
        List<VariableField> variableFields = almaMarcRecord.getVariableFields(tag);
        for (VariableField vf : variableFields) {
            try {
                List<VariableField> variableFields1 = marcRecord.find(bibNumber);
                DataField df= (DataField)variableFields1.get(0);
                return df.getSubfield('a');
            }catch (Exception e){
                log.info("DataField {} was not found", tag);
            }
        }
        return null;
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

