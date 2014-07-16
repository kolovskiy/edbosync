package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonAddRet;
import ua.edboservice.ArrayOfDPersonAddresses2;
import ua.edboservice.ArrayOfDPersonsFind2;
import ua.edboservice.DPersonAddRet;
import ua.edboservice.DPersonAddresses2;
import ua.edboservice.DPersonsFind2;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для синхронизации основных седений о персоне с ЕДБО
 *
 * @author Сергей Чопоров
 * @version 1.0.0
 * @date 26.05.2014
 */
public class EdboPerson {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();
    /**
     * Экземпляр соединения с базой данных
     */
    protected DataBaseConnector db = new DataBaseConnector();
    /**
     * Экземпляр потока SOAP
     */
    protected EDBOPersonSoap soap = edbo.getSoap();
    /**
     * GUID сессии
     */
    protected String sessionGuid = edbo.getSessionGuid();
    /**
     * Текущая дата
     */
    protected String actualDate = edbo.getActualDate();
    /**
     * Идентификатор языка
     */
    protected int languageId = edbo.getLanguageId();

    /**
     * Поиск в базе ЕДБО по серии и номеру документа
     *
     * @param series Серия документа
     * @param number Номер документа
     * @return Сведения о персоне в формате json
     */
    public String find(String series, String number) {
        ArrayOfDPersonsFind2 personArray = soap.personsFind2(sessionGuid, actualDate, languageId, "*", series, number, "", 1, "", "");
        return processArrayOfDPersonsFind(personArray);
    }

    /**
     * Поиск в базе ЕДБО по маске ФИО
     *
     * @param fio Маска ФИО
     * @return Сведения о персоне в формате json
     */
    public String find(String fio) {
        ArrayOfDPersonsFind2 personArray = soap.personsFind2(sessionGuid, actualDate, languageId, fio.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"), "", "", "", 1, "", "");
        return processArrayOfDPersonsFind(personArray);
    }

    /**
     * Обработать массив информации о результатх поиска персоны
     *
     * @param personArray Массив информации о результатах поиска персоны
     * @return Результат разбора массива информации о результатах поиска персоны
     * в формате json
     */
    protected String processArrayOfDPersonsFind(ArrayOfDPersonsFind2 personArray) {
        if (personArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }

        List<DPersonsFind2> personList = personArray.getDPersonsFind2();
        if (personList.size() > 0) {
            ArrayList<Person> person;
            person = new ArrayList<Person>();
            for (DPersonsFind2 dPerson : personList) {
                Person p = new Person();
                // базовая информация
                p.setFirstName(dPerson.getFirstName());
                p.setMiddleName(dPerson.getMiddleName());
                p.setLastName(dPerson.getLastName());
                p.setId_PersonSex(dPerson.getIdPersonSex());
                p.setBirthday(dPerson.getBirthday().toGregorianCalendar());
                p.setResident(dPerson.getResident());
                p.setPersonCodeU(dPerson.getPersonCodeU());
                p.setId_Person(dPerson.getIdPerson());
                p.setFirstNameEn(dPerson.getFirstNameEn());
                p.setLastNameEn(dPerson.getLastNameEn());
                p.setMiddleNameEn(dPerson.getMiddleNameEn());

                // адрес персоны
                ArrayOfDPersonAddresses2 adressesArray = soap.personAddressesGet2(sessionGuid, actualDate, languageId, dPerson.getPersonCodeU(), 0);
                if (adressesArray == null) {
                    // возникла ошибка при получении данных из ЕДБО
                    return edbo.processErrorsJson();
                }
                List<DPersonAddresses2> adressesList = adressesArray.getDPersonAddresses2();
                if (adressesList.size() > 0) {

                    DPersonAddresses2 adress = adressesList.get(0); // работаем с первым адресом (нам болше и не нужно)
                    p.setId_StreetType(adress.getIdStreetType());
                    p.setAddress(adress.getAdress());
                    p.setHomeNumber(adress.getHomeNumber());
                    p.setPostIndex(adress.getPostIndex());
                    p.setId_Country(adress.getIdCountry());
                    p.setApartment(adress.getApartment());
                    p.setHousing(adress.getHousing());
                    // определение кодов KOATUU
                    try {
                        ResultSet koatuu_full = db.executeQuery("SELECT idKOATUULevel3 "
                                + "FROM koatuulevel3 WHERE KOATUULevel3FullName like \""
                                + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                + "\";");
                        if (koatuu_full.next()) {
                            // Запрос вернул информацию о коде KOATUU на третьем уровне
                            p.setId_KoatuuCode(koatuu_full.getInt(1));
                        } else {
                            // Если запись на третьем уровне отсутствует (например, у жителей крупных городов),
                            // то поиск осуществляем по второму уровню
                            ResultSet koatuu2 = db.executeQuery("SELECT idKOATUULevel2 "
                                    + "FROM koatuulevel2 WHERE KOATUULevel2FullName like \""
                                    + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                    + "\";");
                            if (koatuu2.next()) {
                                // Запрос вернул информацию о кодах KOATUU на втором уровне
                                p.setId_KoatuuCode(koatuu2.getInt(1));
                            } else {
                                // на первом и втором уровнях отсутствуют только нерезиденты
                                p.setId_KoatuuCode(135607);
                            }
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                person.add(p);
            }
            Gson json;
            json = new Gson();
            return json.toJson(person);
        }
        return "0";
    }

    /**
     * Добавить персону в базу ЕДБО
     *
     * @param personIdMySql Идентификатор персоны в базе MySql
     * @param entrantDocumentIdMySql Идентификатор документа об образовании в
     * базе MySQL
     * @param personalDocumentIdMySql Идентификатор документа об удостоверении
     * личности в базе MySQL
     * @return Статус попытки в формате json
     * @see edbosync.SubmitStatus
     */
    public String add(int personIdMySql, int entrantDocumentIdMySql, int personalDocumentIdMySql) {
        SubmitStatus submitStatus = new SubmitStatus();
        String personCodeU = "";
        int personIdEdbo = -1;
        Gson json = new Gson();
        String sql = "SELECT * FROM person WHERE idPerson = " + personIdMySql + ";";
        try {
            ResultSet person = db.executeQuery(sql);
            if (person.next()) {
                personCodeU = person.getString("codeU");
                personIdEdbo = person.getInt("edboID");

                if (personCodeU != null && (!personCodeU.isEmpty())) {
                    submitStatus.setError(false);
                    submitStatus.setGuid(personCodeU);
                    submitStatus.setId(personIdEdbo);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage("У персоны пристуствует код ЕДБО. Необходимость добавдения отсутствует.");
                    return json.toJson(submitStatus);
                }
                String birthday = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(person.getDate("Birthday"));
                int personSexId = person.getInt("PersonSexID");
                String firstName = person.getString("FirstName").replace("'", "`");
                String middleName = person.getString("MiddleName").replace("'", "`");
                String lastName = person.getString("LastName").replace("'", "`");
                int resident = person.getInt("IsResident");
                String adress = person.getString("Address");
                String homeNumber = person.getString("HomeNumber");
                String postIndex = person.getString("PostIndex");
                int idStreetType = person.getInt("StreetTypeID");
                int languageIdPerson = person.getInt("LanguageID");
                String birthPlace = person.getString("BirthPlace");
                int countryId = person.getInt("CountryID");
                String koatuu = person.getString("KOATUUCode");
                String firstNameEn = person.getString("FirstNameEn");
                String middleNameEn = person.getString("MiddleNameEn");
                String lastNameEn = person.getString("LastNameEn");
                String apartment = person.getString("Apartment");
                String housing = person.getString("Housing");

                // данные документа об образовании
                String sqlEntrantDocument = "SELECT * FROM documents WHERE `documents`.`idDocuments` = " + entrantDocumentIdMySql + ";";
                ResultSet entrantDocument = db.executeQuery(sqlEntrantDocument);
                if (!entrantDocument.next()) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор документа об образовании");
                    submitStatus.setMessage("Неверный идентификатор документа об образовании");
                    return json.toJson(submitStatus);
                }
                String entrantDocumentSeries = entrantDocument.getString("Series");
                String entrantDocumentNumber = entrantDocument.getString("Numbers");
                String entrantDocumentDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(entrantDocument.getDate("DateGet"));
                String entrantDocumentValue = Float.toString(entrantDocument.getFloat("AtestatValue"));
                String entrantDocumentIssued = entrantDocument.getString("Issued");
                int entrantDocumentAwardTypeId = entrantDocument.getInt("PersonDocumentsAwardsTypesID");
                int entrantDocumentTypeId = entrantDocument.getInt("TypeID");
                int isForeigner = entrantDocument.getInt("isForeinghEntrantDocument");
                int isNotCheckAttestat = entrantDocument.getInt("isNotCheckAttestat");

                System.out.println(entrantDocumentValue);

                // удостоверение личности
                String sqlPersonalDocument = "SELECT * FROM documents WHERE `documents`.`idDocuments` = " + personalDocumentIdMySql + ";";
                ResultSet personalDocument = db.executeQuery(sqlPersonalDocument);
                if (!personalDocument.next()) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор документа об удостоверении личности");
                    submitStatus.setMessage("Неверный идентификатор документа об удостоверении личности");
                    return json.toJson(submitStatus);
                }
                String documentSeries = personalDocument.getString("Series");
                String documentNumber = personalDocument.getString("Numbers");
                String documentDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(personalDocument.getDate("DateGet"));
                String documentIssued = personalDocument.getString("Issued");
                int documentTypeId = personalDocument.getInt("TypeID");

//                    System.out.println(documentSeries + documentNumber + documentDate + documentIssued + documentTypeId);
                String sqlLanguage = "SELECT * FROM languages WHERE idLanguages = " + languageIdPerson + ";";
                ResultSet languageResult = db.executeQuery(sqlLanguage);
                if (!languageResult.next()) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор иностранного языка персоны");
                    submitStatus.setMessage("Неверный идентификатор иностранного языка персоны");
                    return json.toJson(submitStatus);
                }
                String languages = languageResult.getString("LanguagesName");

                String sqlContacts = "SELECT * FROM personcontacts WHERE PersonID = " + personIdMySql + ";";
                ResultSet contacts = db.executeQuery(sqlContacts);
                String phone = "";
                String mobile = "";
                while (contacts.next()) {
                    if (contacts.getInt("PersonContactTypeID") == 1) {
                        phone = contacts.getString("Value");
                    }
                    if (contacts.getInt("PersonContactTypeID") == 2) {
                        mobile = contacts.getString("Value");
                    }
                }
                // проверка наличи в ЕДБО
//                ArrayOfDPersonsFind2 personArray2 = soap.personsFind2(sessionGuid, actualDate, languageId, "*", "", entrantDocumentNumber, "", 1, "", "");
//                if (personArray2 != null) {
//                    List<DPersonsFind2> personList2 = personArray2.getDPersonsFind2();
//                    String fio = "";
//                    String bith = "";
//                    for (DPersonsFind2 dpf : personList2){
//                        personCodeU = dpf.getPersonCodeU();
//                        personIdEdbo = dpf.getIdPerson();
//                        fio = dpf.getFIO();
//                        bith = dpf.getBirthday().toString();
//                        System.out.println(dpf.getPersonCodeU() + dpf.getFirstName());
//                    }
//                    submitStatus.setMessage("Персона з документом для вступу:" + entrantDocumentSeries + " " + entrantDocumentNumber + " знайдена в ЄДЕБО:"
//                            + fio + " " + bith + "р.н."
//                            + " Уважно перевірте введені дані! Наступного разу обов’язково використовуйте пошук перед додаванням персони!");
//
//                } else {
                    // загрузка персоны
                    ArrayOfDPersonAddRet personRetArray = soap.personEntrantAdd2(
                            sessionGuid, // SessionGUID
                            languageId, // Id_Language
                            resident, // Resident
                            birthday, //Birthday
                            personSexId, // Id_PersonSex
                            firstName, // FirtName
                            middleName, // MiddleName
                            lastName, // LastName
                            koatuu, //KOATUUCode
                            idStreetType, // Is_StreetType
                            adress, // Adress
                            homeNumber, // HomeNumber
                            entrantDocumentSeries, // EntrantDocumentSeries
                            entrantDocumentNumber, // EntrantDocumentNumber
                            entrantDocumentDate, // EntrantDocumentDate
                            entrantDocumentValue, // EntrantDocumentValue
                            (documentTypeId == 3) ? documentSeries : "", // PasportSeries
                            (documentTypeId == 3) ? documentNumber : "", // PasportNumber
                            (documentTypeId == 3) ? documentIssued : "", // PasportIssued
                            (documentTypeId == 3) ? documentDate : "", // PasportDate
                            "", // Kode_School
                            phone, // Phone
                            mobile, // Mobile
                            "", // Email
                            "", // Skype
                            "", // ICQ
                            isForeigner, // IsForeinghEntrantDocumet
                            isNotCheckAttestat, // IsNotCheckAttestat
                            entrantDocumentTypeId, // Id_EntrantDocumnetType
                            "", // EntrantDocumnetUniversityKode,
                            "", // Father
                            "", // Mother
                            "", // FatherPhones
                            "", // MotherPhones
                            postIndex, // PostIndex
                            birthPlace, // Birthplace
                            languages, // LanguagesAreStudied
                            entrantDocumentIssued, // EntrantDocumentIssued,
                            entrantDocumentAwardTypeId, // Id_EntrantDocumentsAwardType
                            1, // AllowProcessedPersonalData
                            countryId, // Id_Country
                            -2, // Id_ForeignCountry
                            documentTypeId, // Id_PersonDocumnetType_Pasport
                            firstNameEn, // FirstNameEn
                            middleNameEn, // MiddleNameEn
                            lastNameEn, // LastNameEn
                            apartment, // Apartment
                            housing); // Housing
                    System.out.println(entrantDocumentTypeId + " " + documentTypeId );
                    if (personRetArray == null) {
                        submitStatus.setMessage("Помилка додавання персони в ЄДЕБО:" + edbo.processErrors());
                        return json.toJson(submitStatus);
                    }
                    List<DPersonAddRet> personRetList = personRetArray.getDPersonAddRet();
                    for (DPersonAddRet personRet : personRetList) {
                        System.out.println(personRet.getPersonCodeU());
                        personCodeU = personRet.getPersonCodeU();
                        personIdEdbo = personRet.getIdPerson();
                    }
                }
                // Обновление кода и идентификатора персоны             
                String sqlUpdatePersonCode = "UPDATE `person`\n"
                        + "SET\n"
                        + "`codeU` = \"" + personCodeU + "\",\n"
                        + "`edboID` = " + personIdEdbo + "\n"
                        + "WHERE `idPerson` = " + personIdMySql + ";";
                db.executeUpdate(sqlUpdatePersonCode);
                EdboDocuments edboDocuments = new EdboDocuments();
                edboDocuments.sync(personIdMySql);
//            }
        } catch (SQLException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
        }
//            mySqlConnectionClose();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        submitStatus.setGuid(personCodeU);
        submitStatus.setId(personIdEdbo);
        return json.toJson(submitStatus);
    }
}
