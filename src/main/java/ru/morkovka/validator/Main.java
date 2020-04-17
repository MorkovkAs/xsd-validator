package ru.morkovka.validator;

import org.apache.commons.io.FileUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.morkovka.validator.LoggingUtils.createLogger;

public class Main {
    // Путь к xsd схеме
    private static URL urlPathForXsd = Main.class.getResource("/schemas_4_0/registry_record.xsd");

    // Путь к каталогу валидируемых xml файлов
    private static String pathForXml = "C:\\Users\\Anton Klimakov\\IdeaProjects\\xsd-validator\\src\\main\\resources\\xmls\\07_04_2020";

    // Коллекция для хранения ошибок в отдельно взятом xml файле
    private static ArrayList<SAXException> fileValidationErrorList;

    // Коллекция для хранения результатов группировки ошибок среди всех xml файлов
    private static HashMap<String, Integer> groupedByErrorMap;

    private static boolean isActiveFullErrorLogging = true;

    private static boolean isActiveValidFilesCopying = true;

    private static Logger logger;

    /**
     * Checks validity of xml files against xsd schema
     *
     * @param args required values:
     *             arg[0] - path to xsd scheme, required param, string
     *             arg[1] - absolute path to validated xml files, required param, string
     *             arg[2] - flag to enable logging of all errors, optional param, boolean, defaults to true
     *             arg[3] - flag to enable copying valid files to "valid" directory, optional param, boolean, defaults to true
     */
    public static void main(String[] args) {

        String stringPathForXsd = null;

        if (args.length == 4) {
            stringPathForXsd = args[0];
            pathForXml = args[1];
            isActiveFullErrorLogging = Boolean.parseBoolean(args[2]);
            isActiveValidFilesCopying = Boolean.parseBoolean(args[3]);
        } else if (args.length == 3) {
            stringPathForXsd = args[0];
            pathForXml = args[1];
            isActiveFullErrorLogging = Boolean.parseBoolean(args[2]);
        } else if (args.length == 2) {
            // Переданы значение до xsd и xml файлов
            stringPathForXsd = args[0];
            pathForXml = args[1];
        } else {
            System.out.println("Error. Required params are not filled.\n" +
                    "You should enter values:\n" +
                    "arg[0] - absolute path to xsd scheme, required param, string\n" +
                    "arg[1] - absolute path to validated xml files, required param, string\n" +
                    "arg[2] - flag to enable logging of all errors, optional param, boolean, defaults to true\n" +
                    "arg[3] - flag to enable copying valid files to \"valid\" directory, optional param, boolean, defaults to true\n");
            return;
        }

        if (logger == null) {
            logger = createLogger();
        }
        if (logger == null) {
            System.out.println("Can't create logger. Exit.");
            return;
        }

        // Распечатка входных параметров
        printInputArgs(stringPathForXsd);

        try {
            if (stringPathForXsd != null && !stringPathForXsd.isEmpty()) {
                urlPathForXsd = Paths.get(stringPathForXsd).toUri().toURL();
            }
            Validator validator = createXsdValidator(urlPathForXsd);
            validateAllFilesInDirectory(validator, pathForXml);
        } catch (SAXException | IOException e) {
            System.out.println("Something went wrong...");
            e.printStackTrace();
        }
    }

    private static void printInputArgs(String stringPathForXsd) {
        System.out.println("Input params:\n" +
                "pathForXsd = " + (stringPathForXsd != null ? stringPathForXsd : "/schemas_4_0/registry_record.xsd") + "\n" +
                "pathForXml = " + pathForXml + "\n" +
                "isActiveFullErrorLogging = " + isActiveFullErrorLogging + "\n" +
                "isActiveValidFilesCopying = " + isActiveValidFilesCopying + "\n");

        logger.log(Level.CONFIG, LoggingUtils.getFullLog("Input params:\n" +
                "pathForXsd = " + (stringPathForXsd != null ? stringPathForXsd : "/schemas_4_0/registry_record.xsd") + "\n" +
                "pathForXml = " + pathForXml + "\n" +
                "isActiveFullErrorLogging = " + isActiveFullErrorLogging + "\n" +
                "isActiveValidFilesCopying = " + isActiveValidFilesCopying + "\n"));
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

        System.out.println("Getting all files in " + dir.getCanonicalPath() + ". It can take a while...");
        List<File> files = (List<File>) FileUtils.listFiles(dir, null, true);

        System.out.println("Total count is " + files.size() + "\n");
        logger.log(Level.INFO, LoggingUtils.getFullLog("Total count is " + files.size()));

        for (File file : files) {
            try {
                checkIsValidFile(validator, file);
            } catch (IOException e) {
                System.out.println("Something went wrong...");
                e.printStackTrace();
            }
        }

        // Пишем в лог результат группировки по ошибкам: |кол-во вхождений|текст ошибки|
        groupedByErrorMap.forEach((key, value) -> logger.log(Level.INFO, LoggingUtils.getFullLog(value + "\t " + key)));
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

        // Создание валидатора и коллеций для схемы
        Validator validator = schema.newValidator();
        fileValidationErrorList = new ArrayList<>();
        groupedByErrorMap = new HashMap<>();

        // Создание своего обработчика ошибок, который будет заполнять списки по мере обнаружения ошибок
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                saveErrors(exception);
            }

            @Override
            public void fatalError(SAXParseException exception) {
                saveErrors(exception);
            }

            @Override
            public void error(SAXParseException exception) {
                saveErrors(exception);
            }
        });

        return validator;
    }

    /**
     * Groups all errors by error string
     *
     * @param exception error
     */
    private static void saveErrors(SAXParseException exception) {

        fileValidationErrorList.add(exception);

        // Определяем наличие данной ошибки валидации ранее
        Integer count = groupedByErrorMap.get(exception.getMessage());
        // Ошибка новая, необходимо созранить
        if (count == null) {
            groupedByErrorMap.put(exception.getMessage(), 1);
        } else {
            // Ошибка ранее уже была, увеличиваем счетчик
            groupedByErrorMap.put(exception.getMessage(), count + 1);
        }
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
            if (fileValidationErrorList.isEmpty()) {
                System.out.println(xml.getName() + "\t is valid.");
                // При необходимости копируем валидный файл в отдельную директорию
                if (isActiveValidFilesCopying) {
                    FileUtils.copyFileToDirectory(xml, new File(pathForXml + "/valid"));
                }
                return true;
            } else {
                System.out.println(xml.getName() + "\t is NOT valid.");

                // При необходимости печатаем лог со всеми найденными ошибками в файле
                if (isActiveFullErrorLogging) {
                    for (SAXException ex : fileValidationErrorList) {
                        logger.log(Level.WARNING, LoggingUtils.getFullLog(xml.getName() + "\t" + ex.getMessage()));
                    }
                }

                // Очистка списка хранения ошибок в файле
                fileValidationErrorList = new ArrayList<>();
                return false;
            }
        } catch (SAXException ex) {
            System.out.println(xml.getName() + "\t Something Failed");
            logger.log(Level.SEVERE, LoggingUtils.getFullLog(xml.getName() + "\t Something Failed"));

            // Очистка списка хранения ошибок в файле
            fileValidationErrorList = new ArrayList<>();
            return false;
        }
    }
}
