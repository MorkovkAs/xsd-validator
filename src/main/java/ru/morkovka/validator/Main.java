package ru.morkovka.validator;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.morkovka.validator.LoggingUtils.createLogger;

public class Main {
    private static String pathForXml;
    private static Logger logger;

    public static void main(String[] args) {

        String pathForXsd = null;
        URL xsd = null;

        if (args.length == 1) {
            // Передано значение только до xml файлов. Используем xsd схему из ресурсов
            pathForXml = args[0];
            xsd = Main.class.getResource("/schemas_4_0/registry_record.xsd");
        } else if (args.length == 2) {
            // Переданы значение до xsd и xml файлов
            pathForXsd = args[0];
            pathForXml = args[1];
        } else {
            System.out.println("Params are not correct");
            //pathForXml = "src/main/resources/xmls";
            return;
        }

        if (logger == null) {
            logger = createLogger();
        }
        if (logger == null) {
            return;
        }

        try {
            if (xsd == null && pathForXsd != null && !pathForXsd.isEmpty()) {
                xsd = Paths.get(pathForXsd).toUri().toURL();
            }
            Validator validator = createXsdValidator(xsd);
            validateAllFilesInDirectory(validator, pathForXml);
        } catch (SAXException | IOException e) {
            System.out.println("Something went wrong...");
            e.printStackTrace();
        }
    }

    /**
     * Checks files validity in directory
     *
     * @param validator is a validator corresponded to xsd schema
     * @param path      directory to check files in
     * @throws IOException
     */
    private static void validateAllFilesInDirectory(Validator validator, String path) throws IOException {

        File dir = new File(path);

        System.out.println("Getting all files in " + dir.getCanonicalPath());
        List<File> files = (List<File>) FileUtils.listFiles(dir, null, true);
        System.out.println("Total count is " + files.size());
        for (File file : files) {
            try {
                checkIsValidFile(validator, file);
            } catch (IOException e) {
                System.out.println("Something went wrong...");
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates xsd validator for given xsd schema
     *
     * @param xsd URL corresponded to xsd schema
     * @return xsd validator
     * @throws SAXException
     */
    private static Validator createXsdValidator(URL xsd) throws SAXException {

        // Поиск и создание экземпляра фабрики XML Schema
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        // Компиляция схемы
        // Схема, загруженная в объект типа java.io.File
        Schema schema = factory.newSchema(xsd);

        // Создание валидатора для схемы
        return schema.newValidator();
    }

    /**
     * Checks file validity
     *
     * @param validator is a validator corresponded to xsd schema
     * @param xml       file to validate
     * @return true if xml is valid, false otherwise
     * @throws IOException
     */
    private static boolean checkIsValidFile(Validator validator, File xml) throws IOException {

        // Разбор проверяемого документа
        Source source = new StreamSource(xml);

        // Валидация документа
        try {
            validator.validate(source);
            System.out.println(xml.getName() + "\t is valid.");
            FileUtils.copyFileToDirectory(xml, new File(pathForXml + "/valid"));
            return true;
        } catch (SAXException ex) {
            System.out.println(xml.getName() + "\t is NOT valid");
            logger.log(Level.WARNING, LoggingUtils.getFullLog(ex.getMessage()));
            return false;
        }
    }
}
