package edbosync;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import java.sql.Date;
import java.util.Calendar;
import ua.edboservice.ArrayOfDOlympiadsAwards;
import ua.edboservice.ArrayOfDPersonAddRet;
import ua.edboservice.ArrayOfDPersonRequestSeasons;
import ua.edboservice.ArrayOfDPersonsFind;
import ua.edboservice.DPersonRequestSeasons;
import ua.edboservice.DPersonsFind;
import ua.edboservice.EDBOGuides;
import ua.edboservice.EDBOGuidesSoap;
import ua.edboservice.EDBOPerson;
import ua.edboservice.EDBOPersonSoap;
import ua.edboservice.ArrayOfDPersonAddresses;
import ua.edboservice.ArrayOfDPersonBenefits;
import ua.edboservice.ArrayOfDPersonContacts;
import ua.edboservice.ArrayOfDPersonCourses;
import ua.edboservice.ArrayOfDPersonDocuments;
import ua.edboservice.ArrayOfDPersonDocumentsSubjects;
import ua.edboservice.ArrayOfDPersonOlympiadsAwards;
import ua.edboservice.ArrayOfDPersonRequestStatusTypes;
import ua.edboservice.ArrayOfDPersonRequests;
import ua.edboservice.ArrayOfDPersonRequestsStatuses;
import ua.edboservice.ArrayOfDRequestExaminationCauses;
import ua.edboservice.ArrayOfDSpecRedactions;
import ua.edboservice.ArrayOfDUniversityCourses;
import ua.edboservice.ArrayOfDUniversityFacultetSpecialities;
import ua.edboservice.ArrayOfDUniversityFacultets;
import ua.edboservice.DOlympiadsAwards;
import ua.edboservice.DPersonAddRet;
import ua.edboservice.DPersonAddresses;
import ua.edboservice.DPersonBenefits;
import ua.edboservice.DPersonContacts;
import ua.edboservice.DPersonCourses;
import ua.edboservice.DPersonDocuments;
import ua.edboservice.DPersonDocumentsSubjects;
import ua.edboservice.DPersonOlympiadsAwards;
import ua.edboservice.DPersonRequestStatusTypes;
import ua.edboservice.DPersonRequests;
import ua.edboservice.DPersonRequestsStatuses;
import ua.edboservice.DRequestExaminationCauses;
import ua.edboservice.DSpecRedactions;
import ua.edboservice.DUniversityCourses;
import ua.edboservice.DUniversityFacultetSpecialities;
import ua.edboservice.DUniversityFacultets;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Синхронизатор с EDBO
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 * @see java.util.AbstractList
 * @see java.util.List
 * @see com.google.gson.Gson
 * @see edbosync.Person
 * @see edbosync.PersonContact
 */
public class Synchronizer {

    private EDBOGuides guidesEdbo = new EDBOGuides();
    /**
     * Соап-поток справочников ЕДБО
     */
    private EDBOGuidesSoap guidesSoap = null;
    // Person soap
    private EDBOPerson personEdbo = new EDBOPerson();
    /**
     * Соап-поток информации о персоне базы ЕДБО
     */
    private EDBOPersonSoap personSoap = null;
    /**
     * Идентификатор текущей соап-сессии
     */
    private String sessionGuid = "";
    /**
     * Текущая дата
     */
    private String actualDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Calendar.getInstance().getTime());
    /**
     * Идентификатор языка (1 - укр)
     */
    private int languageId = 1;
    /*
     * Идентификатор вступительной компании (2 -- 2012, 3 -- 2013)
     */
    private int seasonId = 3;
    /**
     * Ключ университета в Soap
     */
    private String universityKey = "ab1bc732-51f3-475c-bcfe-368363369020";
    // MySQL
    /**
     * Обработчик соединения MySQL
     */
    private Connection mySqlConnection = null;
    /**
     * Обработчик запросов MySQL
     */
    private Statement mySqlStatement = null;

    /**
     * Конструктор
     */
    public Synchronizer() {
        System.out.println("Создан объект-синхронизатор EDBO: " + actualDate);
    }

    /**
     * Установка соединения со службой EDBO Guides
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean guidesConnect() {
        SoapConnectionData data = new SoapConnectionData();
        guidesSoap = guidesEdbo.getEDBOGuidesSoap();
        sessionGuid = guidesSoap.login(data.getSoapUser(), data.getSoapPassword(), 0, data.getApplicationKey());
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.out.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Установка соединения со службой EDBO Person
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean personConnect() {
        // wsdl connection url:
        // http://10.1.103.99:8080/EDBOPerson/EDBOPerson.asmx?WSDL
        SoapConnectionData data = new SoapConnectionData();
        personSoap = personEdbo.getEDBOPersonSoap();
        sessionGuid = personSoap.login(data.getSoapUser(), data.getSoapPassword(), 0, data.getApplicationKey());
//        sessionGuid = personSoap.login("davidovskij.v@edbo.gov.ua", "testpass1917", 0, ""); //// TEST !!!!!!!!!!!!!!!!
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.out.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Соединение с тестовой базой данных
     *
     * @return true, если соединение установлено, false - иначе
     */
//    protected boolean personConnectTest() {
//        // wsdl connection url:
//        // http://test.edbo.gov.ua:8080/EDBOPerson/EDBOPerson.asmx?WSDL
//        personSoap = personEdbo.getEDBOPersonSoap();
//        sessionGuid = personSoap.login("davidovskij.v@edbo.gov.ua", "testpass1917", 0, "");
//        if (sessionGuid.length() != 36) {
//            // при соединении возникла ошибка
//            System.out.println(sessionGuid);
//            return false;
//        }
//        return true;
//    }
    /**
     * Установить соединение с базой данных MySQL
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean mySqlConnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            try {
                try {
                    MySqlConnectionData data = new MySqlConnectionData();
                    mySqlConnection = DriverManager.getConnection(data.getMySqlConnectionUrl(), data.getMySqlUser(), data.getMySqlPassword());
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
                mySqlStatement = mySqlConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_UPDATABLE);
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * Закрыть соединение с базой данных MySQL
     */
    protected void mySqlConnectionClose() {
        try {
            mySqlConnection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Список вступительных компаний
     */
    public void personRequestSeasons() {
        if (personConnect()) {
            ArrayOfDPersonRequestSeasons seasonsArray = personSoap.personRequestSeasonsGet(sessionGuid, languageId, "", 0, 0, 1);
            List<DPersonRequestSeasons> seasonsList = seasonsArray.getDPersonRequestSeasons();
            for (DPersonRequestSeasons season : seasonsList) {
                System.out.println(season.getName() + "\t" + season.getRequestPerPerson()
                        + "\t" + season.getClosed() + "\tid: " + season.getIdPersonRequestSeasons());
            }
        }
    }

    /**
     * Загрузка списка специальносте из базы ЕДБО
     */
    public void specialitiesGet() {
        if (guidesConnect() && mySqlConnect()) {
            ArrayOfDUniversityFacultets facultetsArray = guidesSoap.universityFacultetsGet(sessionGuid, universityKey, "", languageId, actualDate, 1, "20", 1, -1, 0, -1);
            List<DUniversityFacultets> facultetsList = facultetsArray.getDUniversityFacultets();
            for (DUniversityFacultets facultet : facultetsList) {
                ArrayOfDUniversityFacultetSpecialities specialitiesArray =
                        guidesSoap.universityFacultetSpecialitiesGet(sessionGuid, universityKey, facultet.getUniversityFacultetKode(), "", languageId, actualDate, seasonId, 0, "", "", "", "");
                List<DUniversityFacultetSpecialities> specialitiesList = specialitiesArray.getDUniversityFacultetSpecialities();
                for (DUniversityFacultetSpecialities speciality : specialitiesList) {
                    String mon_kod = "";
                    if (speciality.getSpecClasifierCode() != null && !speciality.getSpecClasifierCode().isEmpty()) {
                        mon_kod = speciality.getSpecClasifierCode();
                    } else {
                        mon_kod = speciality.getSpecSpecialityClasifierCode();
                    }
                    String sql = "INSERT INTO `abiturient`.`specialities`"
                            + "(`idSpeciality`,"
                            + "`SpecialityName`,"
                            + "`SpecialityDirectionName`,"
                            + "`SpecialitySpecializationName`,"
                            + "`SpecialityKode`,"
                            + "`FacultetID`,"
                            + "`SpecialityClasifierCode`,"
                            + "`SpecialityContractCount`,"
                            + "`PersonEducationFormID`"
                            + ")"
                            + "VALUES"
                            + "("
                            + speciality.getIdUniversitySpecialities() + ","
                            + "'" + speciality.getSpecSpecialityName() + "',"
                            + "'" + speciality.getSpecDirectionName() + "',"
                            + "'" + speciality.getSpecSpecializationName() + "',"
                            + "'" + speciality.getSpecCode() + "',"
                            + facultet.getIdUniversityFacultet() + ","
                            + mon_kod + ","
                            + speciality.getUniversitySpecialitiesContractCount() + ","
                            + speciality.getIdPersonEducationForm()
                            + ");";
                    System.out.println(speciality.getIdUniversitySpecialities() + ","
                            + "'" + speciality.getSpecSpecialityName() + "',"
                            + "'" + speciality.getSpecDirectionName() + "',"
                            + "'" + speciality.getSpecSpecializationName() + "',"
                            + "'" + speciality.getUniversitySpecialitiesKode() + "',"
                            + facultet.getIdUniversityFacultet() + ","
                            + mon_kod + ","
                            + speciality.getUniversitySpecialitiesContractCount() + ","
                            + speciality.getIdPersonEducationForm() + ", begin " + speciality.getDateBeginPersonRequestSeason().toString()
                            + " end " + speciality.getDateEndPersonRequestSeason().toString());
                    System.out.println("");
                    try {
                        ResultSet facultetInMysql = mySqlStatement.executeQuery("SELECT * FROM abiturient.facultets WHERE facultets.idFacultet = "
                                + facultet.getIdUniversityFacultet() + ";");
                        if (!facultetInMysql.next()) {
                            System.out.println("ОШИБКА ФАКУЛЬТЕТА!!!! " + sql);
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
//                    try {
////                        mySqlStatement.executeUpdate(sql);
//                        mySqlStatement.executeUpdate("UPDATE `abiturient`.`specialities`\n"
//                                + "SET\n"
//                                + "SpecialityKode = \"" + speciality.getUniversitySpecialitiesKode() + "\"\n"
//                                + "WHERE idSpeciality = " + speciality.getIdUniversitySpecialities() + ";");
//                    } catch (SQLException ex) {
//                        System.out.println(sql);
//                        System.out.flush();
//                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                }
            }
        }
    }

    public void specReadctions() {
        if (guidesConnect()) {
            ArrayOfDSpecRedactions specRedArray = guidesSoap.specRedactionsGet(sessionGuid);
            List<DSpecRedactions> specRedList = specRedArray.getDSpecRedactions();
            for (DSpecRedactions dSpecRed : specRedList) {
                System.out.println(dSpecRed.getSpecRedactionName() + "  " + dSpecRed.getSpecRedactionCode() + "  " + dSpecRed.getIdSpecRedactions());
            }
        }
    }

    /**
     * Поиск персон по серии и номеру документа в базе ЕДБО
     *
     * @param series Серия документа
     * @param number Номер документа
     * @return Строка в формате json с информацией о персоне или "0", если
     * персон не найдено
     * @see edbosync.Person
     */
    public String findPersonEdbo(String series, String number) {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDPersonsFind personArray = personSoap.personsFind(sessionGuid, actualDate, languageId,
                    "*", series, number, "", 1, "", "");
            List<DPersonsFind> personList = personArray.getDPersonsFind();
            if (personList.size() > 0) {
                ArrayList<Person> person;
                person = new ArrayList<Person>();
                for (DPersonsFind dPerson : personList) {
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

                    System.out.println(dPerson.getBirthday().toString());

                    // адрес персоны
                    ArrayOfDPersonAddresses adressesArray = personSoap.personAddressesGet(sessionGuid, actualDate, languageId, dPerson.getPersonCodeU(), 0);
                    List<DPersonAddresses> adressesList = adressesArray.getDPersonAddresses();
                    if (adressesList.size() > 0) {

                        DPersonAddresses adress = adressesList.get(0); // работаем с первым адресом (нам болше и не нужно)
                        p.setId_StreetType(adress.getIdStreetType());
                        p.setAddress(adress.getAdress());
                        p.setHomeNumber(adress.getHomeNumber());
                        p.setPostIndex(adress.getPostIndex());
                        // определение кодов KOATUU
                        try {
                            ResultSet koatuu_full = mySqlStatement.executeQuery("SELECT idKOATUULevel3, KOATUULevel2ID, "
                                    + "(SELECT `koatuulevel2`.`KOATUULevel1ID` FROM koatuulevel2 WHERE `koatuulevel2`.`idKOATUULevel2` = KOATUULevel2ID) "
                                    + "FROM abiturient.koatuulevel3 WHERE KOATUULevel3FullName like \""
                                    + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                    + "\";");
                            if (koatuu_full.next()) {
                                // Запрос вернул информацию о кодах KOATUU на трех уровнях
                                p.setId_KoatuuCodeL3(koatuu_full.getInt(1));
                                p.setId_KoatuuCodeL2(koatuu_full.getInt(2));
                                p.setId_KoatuuCodeL1(koatuu_full.getInt(3));
                            } else {
                                // Если запись на третьем уровне отсутствует (например, у жителей крупных городов),
                                // то поиск осуществляем по второму уровню
                                ResultSet koatuu2 = mySqlStatement.executeQuery("SELECT idKOATUULevel2, KOATUULevel1ID "
                                        + "FROM abiturient.koatuulevel2 WHERE KOATUULevel2FullName like \""
                                        + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                        + "\";");
                                if (koatuu2.next()) {
                                    // Запрос вернул информацию о кодах KOATUU на двух уровнях
                                    p.setId_KoatuuCodeL2(koatuu2.getInt(1));
                                    p.setId_KoatuuCodeL1(koatuu2.getInt(2));
                                } else {
                                    // на первом и втором уровнях отсутствуют только нерезиденты
                                    p.setId_KoatuuCodeL2(135607);
                                }
                            }
                        } catch (SQLException ex) {
                            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

//                    p.setContacts(getPersonContactsEdbo(dPerson.getPersonCodeU()));

                    person.add(p);
                }
                Gson json;
                json = new Gson();
                return json.toJson(person);
            }
//            mySqlConnectionClose();
        }
        return "0";
    }

    /**
     * Поиск персоны по маске фамилия, имя, отчество
     *
     * @param fioMask маска фио
     * @return Строка в формате json с информацией о персоне или "0", если
     * персон не найдено
     * @see edbosync.Person
     */
    public String findPersonEdbo(String fioMask) {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDPersonsFind personArray = personSoap.personsFind(sessionGuid, actualDate, languageId,
                    fioMask.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"), "", "", "", 1, "", "");
//            System.out.println(fioMask.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"));
            List<DPersonsFind> personList = personArray.getDPersonsFind();
            if (personList.size() > 0) {
                ArrayList<Person> person;
                person = new ArrayList<Person>();
                for (DPersonsFind dPerson : personList) {
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

                    // адрес персоны
                    ArrayOfDPersonAddresses adressesArray = personSoap.personAddressesGet(sessionGuid, actualDate, languageId, dPerson.getPersonCodeU(), 0);
                    List<DPersonAddresses> adressesList = adressesArray.getDPersonAddresses();
                    if (adressesList.size() > 0) {

                        DPersonAddresses adress = adressesList.get(0); // работаем с первым адресом (нам болше и не нужно)
                        p.setId_StreetType(adress.getIdStreetType());
                        p.setAddress(adress.getAdress());
                        p.setHomeNumber(adress.getHomeNumber());
                        p.setPostIndex(adress.getPostIndex());
                        // определение кодов KOATUU
                        try {
                            ResultSet koatuu_full = mySqlStatement.executeQuery("SELECT idKOATUULevel3, KOATUULevel2ID, "
                                    + "(SELECT `koatuulevel2`.`KOATUULevel1ID` FROM koatuulevel2 WHERE `koatuulevel2`.`idKOATUULevel2` = KOATUULevel2ID) "
                                    + "FROM abiturient.koatuulevel3 WHERE KOATUULevel3FullName like \""
                                    + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                    + "\";");
                            if (koatuu_full.next()) {
                                // Запрос вернул информацию о кодах KOATUU на трех уровнях
                                p.setId_KoatuuCodeL3(koatuu_full.getInt(1));
                                p.setId_KoatuuCodeL2(koatuu_full.getInt(2));
                                p.setId_KoatuuCodeL1(koatuu_full.getInt(3));
                            } else {
                                // Если запись на третьем уровне отсутствует (например, у жителей крупных городов),
                                // то поиск осуществляем по второму уровню
                                ResultSet koatuu2 = mySqlStatement.executeQuery("SELECT idKOATUULevel2, KOATUULevel1ID "
                                        + "FROM abiturient.koatuulevel2 WHERE KOATUULevel2FullName like \""
                                        + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                        + "\";");
                                if (koatuu2.next()) {
                                    // Запрос вернул информацию о кодах KOATUU на двух уровнях
                                    p.setId_KoatuuCodeL2(koatuu2.getInt(1));
                                    p.setId_KoatuuCodeL1(koatuu2.getInt(2));
                                } else {
                                    // на первом и втором уровнях отсутствуют только нерезиденты
                                    p.setId_KoatuuCodeL2(135607);
                                }
                            }
                        } catch (SQLException ex) {
                            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

//                    p.setContacts(getPersonContactsEdbo(dPerson.getPersonCodeU()));

                    person.add(p);
                }
//                mySqlConnectionClose();
                Gson json;
                json = new Gson();
                return json.toJson(person);
            }
        }
        return "0";
    }

    /**
     * Получить список контактов персоны в базе ЕДБО
     *
     * @param personCodeU Код персоны в ЕДБО
     * @return Список контактов персоны
     * @see edbosync.PersonContact
     */
    public ArrayList<PersonContact> getPersonContactsEdbo(String personCodeU) {
        ArrayList<PersonContact> contacts;
        contacts = new ArrayList<PersonContact>();
        if (personConnect()) {
            ArrayOfDPersonContacts contactsArray = personSoap.personContactsGet(sessionGuid, actualDate, languageId, personCodeU, 0);
            List<DPersonContacts> contactsList = contactsArray.getDPersonContacts();
            for (DPersonContacts dContact : contactsList) {
                PersonContact contact = new PersonContact();
                contact.setId_Contact(dContact.getIdPersonContact());
                contact.setId_ContactType(dContact.getIdPersonContactType());
                contact.setIsDefault(dContact.getDefaull());
                contact.setValue(dContact.getValue());

                contacts.add(contact);
            }
        }
        return contacts;
    }

    /**
     * Получить список контактов персоны из ЕДБО в формате json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список контактов персоны из ЕДБО в формате json
     */
    public String getPersonContactsEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<PersonContact> contacts = getPersonContactsEdbo(personCodeU);
        return json.toJson(contacts);
    }

    /**
     * Получить список документов персоны из базы ЕДБО
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список документов персоны из базы ЕДБО
     * @see edbosync.PersonDocument
     */
    public ArrayList<PersonDocument> getPersonDocumentEdbo(String personCodeU) {
        ArrayList<PersonDocument> documents = new ArrayList<PersonDocument>();
        if (personConnect()) {
            ArrayOfDPersonDocuments documentsArray = personSoap.personDocumentsGet(sessionGuid, actualDate, languageId, personCodeU, 0, 0, "", -1);
            if (documentsArray != null) {
                List<DPersonDocuments> documentsList = documentsArray.getDPersonDocuments();
                for (DPersonDocuments dDocument : documentsList) {
                    PersonDocument document = new PersonDocument();
                    document.setAttestatValue(dDocument.getAtestatValue());
                    document.setDateGet(dDocument.getDocumentDateGet().toGregorianCalendar());
                    document.setId_Document(dDocument.getIdPersonDocument());
                    document.setId_Type(dDocument.getIdPersonDocumentType());
                    document.setIssued(dDocument.getDocumentIssued());
                    document.setNumber(dDocument.getDocumentNumbers());
                    document.setSeries(dDocument.getDocumentSeries());
                    document.setZnoPin(dDocument.getZNOPin());

                    if (document.getId_Type() == 4) {
                        // документ является сертификатом ЗНО
                        ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>();
                        ArrayOfDPersonDocumentsSubjects subjectsArray = personSoap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, document.getId_Document(), dDocument.getIdPerson(), document.getId_Type());
                        List<DPersonDocumentsSubjects> subjectsList = subjectsArray.getDPersonDocumentsSubjects();
                        for (DPersonDocumentsSubjects dSubject : subjectsList) {
                            DocumentSubject subject = new DocumentSubject();
                            subject.setId_Subject(dSubject.getIdSubject());
                            subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                            subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                            subjects.add(subject);
                        }
                        document.setSubjects(subjects);
                    }

                    documents.add(document);
                }
            }
        }
        return documents;
    }

    /**
     * Получить список документов персоны из базы ЕДБО в формате json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список документов персоны из базы ЕДБО в формате json
     * @see edbosync.PersonDocument
     */
    public String getPersonDocumentEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<PersonDocument> documents = getPersonDocumentEdbo(personCodeU);
        return json.toJson(documents);
    }

    /**
     * Получить список идентификаторов олимпиад персоны из базы ЕДБО
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список идентификаторов олимпиад персоны
     */
    public ArrayList<Integer> getPersonOlympiadsEdbo(String personCodeU) {
        ArrayList<Integer> olympiadId = new ArrayList<Integer>();
        if (personConnect()) {
            ArrayOfDPersonOlympiadsAwards awardsArray = personSoap.personOlympiadsAwardsGet(sessionGuid, actualDate, languageId, personCodeU, seasonId);
            if (awardsArray != null) {
                List<DPersonOlympiadsAwards> awardsList = awardsArray.getDPersonOlympiadsAwards();
                for (DPersonOlympiadsAwards award : awardsList) {
                    olympiadId.add(award.getIdOlympiadAward());
                }
            }
        }
        return olympiadId;
    }

    /**
     * Синхронизация списка олимпиад
     *
     * @param personCodeU Код персоны (GUID)
     * @param personId Идентификатор персоны в базе MySQL
     */
    public void syncPersonOlympiadsEdbo(String personCodeU, int personId) {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDPersonOlympiadsAwards awardsArray = personSoap.personOlympiadsAwardsGet(sessionGuid, actualDate, languageId, personCodeU, seasonId);
            if (awardsArray != null) {
                List<DPersonOlympiadsAwards> awardsList = awardsArray.getDPersonOlympiadsAwards();
                for (DPersonOlympiadsAwards award : awardsList) {
                    String sql = "SELECT * "
                            + "FROM abiturient.personolympiad "
                            + "WHERE PersonID = " + personId + " AND OlympiadAwarID = " + award.getIdOlympiadAward() + ";";
                    try {
                        ResultSet personOlympiadsRS = mySqlStatement.executeQuery(sql);
                        if (personOlympiadsRS.next()) {
                            // олимпиада уже была добавлена
                            personOlympiadsRS.updateInt("edboID", award.getIdPersonOlympiadAward());
                            personOlympiadsRS.updateRow();
                            System.out.println("Обновлена олимпиада: " + award.getIdPersonOlympiadAward());
                        } else {
                            personOlympiadsRS.moveToInsertRow();
                            personOlympiadsRS.updateInt("PersonID", personId);
                            personOlympiadsRS.updateInt("OlympiadAwarID", award.getIdOlympiadAward());
                            personOlympiadsRS.updateInt("edboID", award.getIdPersonOlympiadAward());
                            personOlympiadsRS.insertRow();
                            personOlympiadsRS.moveToCurrentRow();
                            System.out.println("Вставлена олимпиада: " + award.getIdPersonOlympiadAward());
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            String sql = "SELECT * "
                    + "FROM abiturient.personolympiad "
                    + "WHERE PersonID = " + personId + ";";
            try {
                ResultSet olympiad = mySqlStatement.executeQuery(sql);
                while (olympiad.next()) {
                    if (olympiad.getInt("edboID") == 0) {
                        int edboID = personSoap.personOlympiadsAwardsAdd(sessionGuid, languageId, personId, olympiad.getInt("OlympiadAwarID"));
                        olympiad.updateInt("edboID", edboID);
                        olympiad.updateRow();
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Получить список идентификаторов олимпиад персоны из базы ЕДБО в формате
     * json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список идентификаторов олимпиад персоны в формате json
     */
    public String getPersonOlympiadsEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<Integer> olympiadId = getPersonOlympiadsEdbo(personCodeU);
        return json.toJson(olympiadId);
    }

    /**
     * Получить список идентификаторов льгот персоны из базы ЕДБО
     *
     * @param personId идентификатор персоны в базе ЕДБО
     * @return список идентификаторов льгот персоны из базы ЕДБО
     * @see edbosync.PersonBenefit
     */
    public ArrayList<PersonBenefit> getPersonBenefitsEdbo(int personId) {
        ArrayList<PersonBenefit> personBenefits = new ArrayList<PersonBenefit>();
        if (personConnect()) {
            ArrayOfDPersonBenefits benefitsArray = personSoap.personBenefitsGet(sessionGuid, actualDate, languageId, personId);
            if (benefitsArray != null) {
                List<DPersonBenefits> benefitsList = benefitsArray.getDPersonBenefits();
                for (DPersonBenefits dBenefit : benefitsList) {
                    PersonBenefit benefit = new PersonBenefit();
                    benefit.setId_Benefit(dBenefit.getIdBenefit());
                    benefit.setId_PersonBenefit(dBenefit.getIdPersonBenefit());
                    personBenefits.add(benefit);
                }
            }
        }
        return personBenefits;
    }

    /**
     * Получить список идентификаторов льгот персоны из базы ЕДБО в формате json
     *
     * @param personId идентификатор персоны в базе ЕДБО
     * @return список идентификаторов льгот персоны из базы ЕДБО в формате json
     */
    public String getPersonBenefitsEdboJson(int personId) {
        Gson json = new Gson();
        ArrayList<PersonBenefit> benefitId = getPersonBenefitsEdbo(personId);
        return json.toJson(benefitId);
    }

    /**
     * Синхронизация списков причин сдачи экзаменов вместо ЗНО
     */
    public void personRequestExaminationCauses() {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDRequestExaminationCauses causesArray = personSoap.personRequestExaminationCausesGet(sessionGuid, languageId);
            List<DRequestExaminationCauses> causesList = causesArray.getDRequestExaminationCauses();
            for (DRequestExaminationCauses dCause : causesList) {
                String sql = "INSERT INTO `abiturient`.`causality`\n"
                        + "(`idCausality`,\n"
                        + "`CausalityName`,\n"
                        + "`CausalityDescription`)\n"
                        + "VALUES\n"
                        + "(\n"
                        + dCause.getIdPersonRequestExaminationCause() + ",\n"
                        + "'" + dCause.getPersonRequestExaminationCauseName().replaceAll("'", "\\\\'") + "',\n"
                        + "'" + dCause.getPersonRequestExaminationCauseDescription().replaceAll("'", "\\\\'") + "'\n"
                        + ");";
                try {
                    mySqlStatement.executeUpdate(sql);
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
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
    public String addPersonEdbo(int personIdMySql,
            int entrantDocumentIdMySql, int personalDocumentIdMySql) {

        SubmitStatus submitStatus = new SubmitStatus();
        String personCodeU = "";
        int personIdEdbo = -1;
        Gson json = new Gson();
        if (personConnect() && mySqlConnect()) {
            String sql = "SELECT * FROM person WHERE idPerson = " + personIdMySql + ";";
            try {
                ResultSet person = mySqlStatement.executeQuery(sql);
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
                    String firstName = person.getString("FirstName");
                    String middleName = person.getString("MiddleName");
                    String lastName = person.getString("LastName");
                    int resident = person.getInt("IsResident");
                    String adress = person.getString("Address");
                    String homeNumber = person.getString("HomeNumber");
                    String postIndex = person.getString("PostIndex");
                    int idStreetType = person.getInt("StreetTypeID");
                    int languageIdPerson = person.getInt("LanguageID");
                    String birthPlace = person.getString("BirthPlace");
                    int koatuuL1Id = person.getInt("KOATUUCodeL1ID");
                    int koatuuL2Id = person.getInt("KOATUUCodeL2ID");
                    int koatuuL3Id = person.getInt("KOATUUCodeL3ID");
                    int countryId = person.getInt("CountryID");
                    String koatuu = "";
                    ResultSet koatuuL3 = mySqlStatement.executeQuery(
                            "SELECT KOATUULevel3Code "
                            + "FROM koatuulevel3 "
                            + "WHERE "
                            + "idKOATUULevel3 = " + koatuuL3Id + ";");
                    if (koatuuL3.next()) {
                        koatuu = koatuuL3.getString(1);
                    } else {
                        ResultSet koatuuL2 = mySqlStatement.executeQuery(
                                "SELECT KOATUULevel2Code "
                                + "FROM koatuulevel2 "
                                + "WHERE "
                                + "idKOATUULevel2 = " + koatuuL2Id + ";");
                        if (koatuuL2.next()) {
                            koatuu = koatuuL2.getString(1);
                        } else {
                            ResultSet koatuuL1 = mySqlStatement.executeQuery(
                                    "SELECT KOATUULevel1Code "
                                    + "FROM koatuulevel1 "
                                    + "WHERE "
                                    + "idKOATUULevel1 = " + koatuuL1Id + ";");
                            if (koatuuL1.next()) {
                                koatuu = koatuuL1.getString(1);
                            } else {
                                Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "KOATUU of Person ERROR!!!");
                            }
                        }
                    }

                    // данные документа об образовании
                    String sqlEntrantDocument = "SELECT * FROM abiturient.documents WHERE `documents`.`idDocuments` = " + entrantDocumentIdMySql + ";";
                    ResultSet entrantDocument = mySqlStatement.executeQuery(sqlEntrantDocument);
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
                    String sqlPersonalDocument = "SELECT * FROM abiturient.documents WHERE `documents`.`idDocuments` = " + personalDocumentIdMySql + ";";
                    ResultSet personalDocument = mySqlStatement.executeQuery(sqlPersonalDocument);
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

                    String sqlLanguage = "SELECT * FROM abiturient.languages WHERE idLanguages = " + languageIdPerson + ";";
                    ResultSet languageResult = mySqlStatement.executeQuery(sqlLanguage);
                    if (!languageResult.next()) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор иностранного языка персоны");
                        submitStatus.setMessage("Неверный идентификатор иностранного языка персоны");
                        return json.toJson(submitStatus);
                    }
                    String languages = languageResult.getString("LanguagesName");

                    String sqlContacts = "SELECT * FROM abiturient.personcontacts WHERE PersonID = " + personIdMySql + ";";
                    ResultSet contacts = mySqlStatement.executeQuery(sqlContacts);
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
                    // загрузка персоны
                    ArrayOfDPersonAddRet personRetArray = personSoap.personEntrantAdd(
                            sessionGuid, // 1
                            languageId, // 2
                            resident, //3
                            birthday, //4
                            personSexId, // 5
                            firstName, // 6
                            middleName, // 7
                            lastName, // 8
                            koatuu, // 9
                            idStreetType, // 10
                            adress, // 11
                            homeNumber, // 12
                            entrantDocumentSeries, // 13
                            entrantDocumentNumber, // 14
                            entrantDocumentDate, // 15 
                            entrantDocumentValue, // 16
                            (documentTypeId == 3) ? documentSeries : "", // 17 паспорт
                            (documentTypeId == 3) ? documentNumber : "", // 18
                            (documentTypeId == 3) ? documentIssued : "", // 19
                            (documentTypeId == 3) ? documentDate : "", // 20
                            (documentTypeId == 1) ? documentSeries : "", // 21 свидетельство о рождении
                            (documentTypeId == 1) ? documentNumber : "", // 22
                            (documentTypeId == 1) ? documentDate : "", // 23
                            "", // 24
                            phone, // 25
                            mobile, // 26
                            "", // 27
                            "", // 28
                            "", // 29
                            isForeigner, // 30
                            isNotCheckAttestat, // 31
                            entrantDocumentTypeId, // 32
                            "", // 33
                            "", // 34
                            "", // 35
                            "", // 36
                            "", // 37
                            postIndex, // 38
                            birthPlace, // 39 место рождения
                            languages, // 40
                            entrantDocumentIssued, // 41
                            entrantDocumentAwardTypeId, // 42
                            1, // 43
                            (documentTypeId == 17) ? documentSeries : "", // 44
                            (documentTypeId == 17) ? documentNumber : "", // 45
                            (documentTypeId == 17) ? documentIssued : "", // 46
                            (documentTypeId == 17) ? documentDate : "", // 47
                            countryId); //804); // 48
                    if (personRetArray == null) {
                        submitStatus.setMessage(personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        return json.toJson(submitStatus);
                    }
                    List<DPersonAddRet> personRetList = personRetArray.getDPersonAddRet();
                    for (DPersonAddRet personRet : personRetList) {
                        System.out.println(personRet.getPersonCodeU());
                        personCodeU = personRet.getPersonCodeU();
                        personIdEdbo = personRet.getIdPerson();
                    }
                    // Обновление кода и идентификатора персоны             
                    String sqlUpdatePersonCode = "UPDATE `abiturient`.`person`\n"
                            + "SET\n"
                            + "`codeU` = \"" + personCodeU + "\",\n"
                            + "`edboID` = " + personIdEdbo + "\n"
                            + "WHERE `idPerson` = " + personIdMySql + ";";
                    mySqlStatement.executeUpdate(sqlUpdatePersonCode);
                    // Обновление кодов документов
                    ArrayList<PersonDocument> personDocuments = getPersonDocumentEdbo(personCodeU);
                    for (PersonDocument document : personDocuments) {
                        if (entrantDocumentNumber.equalsIgnoreCase(document.getNumber())) {
                            mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
                                    + "SET\n"
                                    + "`edboID` = " + document.getId_Document() + "\n"
                                    + "WHERE idDocuments = " + entrantDocumentIdMySql + ";");
                        }
                        if (documentNumber.equalsIgnoreCase(document.getNumber()) && documentSeries.equalsIgnoreCase(document.getSeries())) {
                            mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
                                    + "SET\n"
                                    + "`edboID` = " + document.getId_Document() + "\n"
                                    + "WHERE idDocuments = " + personalDocumentIdMySql + ";");
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
//            mySqlConnectionClose();
        }
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        submitStatus.setGuid(personCodeU);
        submitStatus.setId(personIdEdbo);
        return json.toJson(submitStatus);
    }

    /**
     * Добавить документы персоны в базу ЕДБО
     *
     * <p>Метод производит перебор документов в базе "Абитуриент". Для каждого
     * документа, у кторого отсутствует идентификатор записи в базе ЕДБО,
     * производится попытка добавления.</p>
     *
     * @param personIdMySql Идентификатор персоны в базе MySQL
     * @return Статус попытки в формате json
     * @see edbosync.SubmitStatus
     */
    public String addPersonDocumentsEdbo(int personIdMySql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        // Массив предметов сертификата
        ArrayList<DocumentSubject> documentSubject = new ArrayList<DocumentSubject>();

        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        if (personConnect() && mySqlConnect()) {
            int edboIdPerson;
            String codeUPerson;
            try {
                // идентификатор персоны в базе ЕДБО
                ResultSet person = mySqlStatement.executeQuery("SELECT `person`.`edboID`, `person`.`codeU` FROM abiturient.person WHERE idPerson = " + personIdMySql + ";");
                person.next();
                edboIdPerson = person.getInt(1);
                codeUPerson = person.getString(2);
                if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage("Неможливо додати документи до персони, яка не пройшла синхронизацію з ЄДБО.");
                    return json.toJson(submitStatus);
                }
                // очистим идентифкатор документа оо образовании для ресинхронизации
                mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
                        + "SET\n"
                        + "`documents`.`edboID` = null\n"
                        + "WHERE `documents`.`TypeID` in (2, 7,8,9,10,11,12,13,14,15) AND `documents`.`PersonID` = " + personIdMySql + ";");
                ArrayList<PersonDocument> personDocuments = getPersonDocumentEdbo(codeUPerson);
                for (PersonDocument personDocument : personDocuments) {
//                    System.out.println(personDocument.getSeries() + personDocument.getNumber());
                    ResultSet documentMySql = mySqlStatement.executeQuery("SELECT * FROM `abiturient`.`documents` "
                            + "WHERE PersonID = " + personIdMySql + " AND Numbers = \"" + personDocument.getNumber() + "\";");
//                    mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
//                            + "SET\n"
//                            + "`edboID` = " + personDocument.getId_Document() + "\n"
//                            //                                    + "WHERE PersonID = " + personIdMySql + " AND Series = \"" + personDocument.getSeries() + "\" AND Numbers = \"" + personDocument.getNumber() + "\";");
//                            + "WHERE PersonID = " + personIdMySql + " AND Numbers = \"" + personDocument.getNumber() + "\";");
                    if (documentMySql.next()) {
                        int docId = documentMySql.getInt("idDocuments");
                        documentMySql.updateInt("edboID", personDocument.getId_Document());
                        documentMySql.updateRow();
                        if (!personDocument.getSubjects().isEmpty()) {
                            for (DocumentSubject docSub : personDocument.getSubjects()) {
                                DocumentSubject sub = docSub;
                                sub.setDocumentId(docId);
                                documentSubject.add(sub);
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }
            String sqlSelectDocuments = "SELECT * FROM abiturient.documents WHERE PersonID = " + personIdMySql + ";";
            ResultSet document;

            try {
                document = mySqlStatement.executeQuery(sqlSelectDocuments);
                while (document.next()) {
                    int idDocument = document.getInt("idDocuments");
                    int typeId = document.getInt("TypeID");
                    String series = document.getString("Series");
                    String number = document.getString("Numbers");
                    String dateGet = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(document.getDate("DateGet"));
                    int znoPin = (typeId == 4) ? document.getInt("ZNOPin") : 0;
                    //float attestatval = (typeId == 11 || typeId == 12) ? document.getFloat("AtestatValue") * 10.0f : document.getFloat("AtestatValue");
                    String attestatValue = Float.toString(document.getFloat("AtestatValue"));
                    String issued = document.getString("Issued");
                    int awardTypeId = document.getInt("PersonDocumentsAwardsTypesID");
                    int edboId = document.getInt("edboID");
                    if (document.wasNull() || edboId == 0) {
                        edboId = (typeId == 4)
                                ? personSoap.personDocumentsZnoAdd(sessionGuid, languageId, edboIdPerson, number, dateGet, znoPin)
                                : personSoap.personDocumentsAdd(sessionGuid,
                                languageId,
                                edboIdPerson,
                                typeId,
                                0,
                                (series != null) ? series : "",
                                (number != null) ? number : "",
                                (dateGet != null) ? dateGet : "",
                                (issued != null) ? issued : "",
                                "",
                                znoPin,
                                attestatValue,
                                1,
                                awardTypeId);
                        if (edboId == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + number + "  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
//                            System.out.println(number + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        }
                        document.updateInt("edboID", edboId);
                        document.updateRow();
                        // если работаем с сертификатом, то добавляем перметы в список
                        if (typeId == 4) {
                            ArrayOfDPersonDocumentsSubjects certificatSubjectsArray = personSoap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, edboId, edboIdPerson, typeId);
                            if (certificatSubjectsArray != null) {
                                List<DPersonDocumentsSubjects> documentSubjectsList = certificatSubjectsArray.getDPersonDocumentsSubjects();
                                for (DPersonDocumentsSubjects dSubject : documentSubjectsList) {
                                    DocumentSubject subject = new DocumentSubject();
                                    subject.setDocumentId(idDocument);
                                    subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                                    subject.setId_Subject(dSubject.getIdSubject());
                                    subject.setSubjectName(dSubject.getSubjectName());
                                    subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                                    documentSubject.add(subject);
                                }
                            }
                        }
                    }
                }
                // Синхронизация списков предметов персоны
                for (DocumentSubject subject : documentSubject) {
                    ResultSet docsubRs = mySqlStatement.executeQuery(""
                            + "SELECT idDocumentSubject, edboID, SubjectValue "
                            + "FROM abiturient.documentsubject "
                            + "WHERE DocumentID = " + subject.getDocumentId() + " AND SubjectID = " + subject.getId_Subject() + ";");
                    if (docsubRs.next()) {
                        // найдена подходящая запись
                        if (docsubRs.getInt("edboID") == 0) {
                            // запись о предмете не была синхронизирована
                            docsubRs.updateDouble("SubjectValue", subject.getSubjectValue());
                            docsubRs.updateInt("edboID", subject.getId_DocumentSubject());
                            docsubRs.updateRow();
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "У сертифікаті оновлено предмет: \" " + subject.getSubjectName() + "\" " + "бал: " + Double.toString(subject.getSubjectValue()) + "<br />");
                        }
                    } else {
                        // вставляем новую запись
                        mySqlStatement.executeUpdate("INSERT INTO `abiturient`.`documentsubject`\n"
                                + "(\n"
                                + "`DocumentID`,\n"
                                + "`SubjectID`,\n"
                                + "`SubjectValue`,\n"
                                + "`edboID`)\n"
                                + "VALUES\n"
                                + "(\n"
                                + subject.getDocumentId() + ",\n"
                                + subject.getId_Subject() + ",\n"
                                + subject.getSubjectValue() + ",\n"
                                + subject.getId_DocumentSubject() + "\n"
                                + ");");
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "До сертифікату додано предмет: \" " + subject.getSubjectName() + "\" " + "бал: " + Double.toString(subject.getSubjectValue()) + "<br />");
                    }
                }

            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }
//            mySqlConnectionClose();
        } else {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка з’єднання.");
            return json.toJson(submitStatus);
        }

//        submitStatus.setMessage("Синхронизацію успіхно завершено.");
        return json.toJson(submitStatus);
    }

    /**
     * Отредактировать параметры документа в базе ЕДБО
     * <p>Метод для документа, который уже синхронизирован с ЕДБО, производит
     * редактирование полей "серия", "номер", "дата выдачи" и "кем выдан" в базе
     * ЕДБО</p>
     *
     * @param documentIdMySql Идентификатор документа в базе MySQL
     * @return Статус попытки в формате json
     * @see edbosync.SubmitStatus
     */
    public String editDocumentEdbo(int documentIdMySql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);

        if (mySqlConnect() && personConnect()) {
            String sql = "SELECT * FROM abiturient.documents WHERE idDocuments = " + documentIdMySql + ";";
            try {
                ResultSet document = mySqlStatement.executeQuery(sql);
                if (document.next()) {
                    int idPersonDocument = document.getInt("edboID");
                    if (idPersonDocument == 0) {
                        submitStatus.setError(true);
                        submitStatus.setMessage("Не можливо редагувати документ, який не синхронизований з ЭДБО");
                        return json.toJson(submitStatus);
                    }
                    int typeId = document.getInt("TypeID");
                    String series = document.getString("Series");
                    String number = document.getString("Numbers");
                    String dateGet = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(document.getDate("DateGet"));
                    String issued = document.getString("Issued");
                    int awardTypeId = document.getInt("PersonDocumentsAwardsTypesID");
                    float attestatval = (typeId == 11 || typeId == 12) ? document.getFloat("AtestatValue") * 10.0f : document.getFloat("AtestatValue");
                    String attestatValue = Float.toString(attestatval);
                    if (personSoap.personDocumentsEdit(
                            sessionGuid,
                            languageId,
                            idPersonDocument,
                            0,
                            (series != null) ? series : "",
                            (number != null) ? number : "",
                            (dateGet != null) ? dateGet : "",
                            (issued != null) ? issued : "",
                            "",
                            1,
                            awardTypeId) == 0) {
                        submitStatus.setError(true);
                        submitStatus.setMessage("Помилка редагування документа: " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        return json.toJson(submitStatus);
                    }
                    // если документ - аттестат, то обновлям сведения о среднем бале в едбо
                    if (typeId == 2 || (typeId >= 7 && typeId <= 15)) {
                        if (personSoap.entrantDocumentValueChange(sessionGuid, attestatValue, 1, universityKey, idPersonDocument) == 0) {
                            submitStatus.setError(true);
                            submitStatus.setMessage("Помилка редагування документа: " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                            return json.toJson(submitStatus);
                        }
                    }
                    submitStatus.setMessage("Документ успішно відредактовано в базі ЄДБО");
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setMessage("Помилка з’єднання з базою даних \"Абітурієнт\"");
            }
        }

        return json.toJson(submitStatus);
    }

    /**
     * Добавление льгот персоны в базу ЕДБО
     *
     * @param personIdMySql Идентификатор персоны в базе Mysql
     * @return Статус попытки добавления льгот в формате json
     * @see edbosync.SubmitStatus
     */
    public String addPersonBenefits(int personIdMySql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);

        if (personConnect() && mySqlConnect()) {
            int edboIdPerson;
            String codeUPerson;
            try {
                // идентификатор персоны в базе ЕДБО
                ResultSet person = mySqlStatement.executeQuery("SELECT `person`.`edboID`, `person`.`codeU` FROM abiturient.person WHERE idPerson = " + personIdMySql + ";");
                person.next();
                edboIdPerson = person.getInt(1);
                codeUPerson = person.getString(2);
                if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage("Неможливо додати льготи до персони, яка не пройшла синхронизацію з ЄДБО.");
                    return json.toJson(submitStatus);
                }
                ArrayList<PersonBenefit> benefitsEdbo = getPersonBenefitsEdbo(edboIdPerson);
                if (!benefitsEdbo.isEmpty()) {
                    for (PersonBenefit benEdbo : benefitsEdbo) {
                        ResultSet benifitMysql = mySqlStatement.executeQuery(""
                                + "SELECT * "
                                + "FROM abiturient.personbenefits "
                                + "WHERE PersonID = " + personIdMySql + " AND BenefitID = " + benEdbo.getId_Benefit() + ";");
                        if (benifitMysql.next()) {
                            benifitMysql.updateInt("edboID", benEdbo.getId_PersonBenefit());
                            benifitMysql.updateRow();
                        } else {
                            benifitMysql.moveToInsertRow();
                            benifitMysql.updateInt("PersonID", personIdMySql);
                            benifitMysql.updateInt("BenefitID", benEdbo.getId_Benefit());
                            benifitMysql.updateInt("edboID", benEdbo.getId_PersonBenefit());
                            benifitMysql.insertRow();
                            benifitMysql.moveToCurrentRow();
                        }
                        benifitMysql.close();
                    }
                }

                ResultSet personBenefits = mySqlStatement.executeQuery(""
                        + "SELECT * "
                        + "FROM abiturient.personbenefits "
                        + "WHERE PersonID = " + personIdMySql + ";");
                while (personBenefits.next()) {
                    if (personBenefits.getInt("edboID") == 0) {
                        // необходимо добавление на сервер
                        int result = personSoap.personBenefitsAdd(sessionGuid, actualDate, languageId, edboIdPerson, personBenefits.getInt("BenefitID"));
                        if (result == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання пільги  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                        } else {
                            personBenefits.updateInt("edboID", result);
                            personBenefits.updateRow();
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }
//            mySqlConnectionClose();
        } else {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка з’єднання.");
            return json.toJson(submitStatus);
        }
        return json.toJson(submitStatus);
    }

    public String addPersonRequestEdbo(int personIdMySql, int personSpeciality) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);

        if (personConnect() && mySqlConnect()) {
            int edboIdPerson;
            String codeUPerson;
            try {
                // идентификатор персоны в базе ЕДБО
                ResultSet person = mySqlStatement.executeQuery("SELECT `person`.`edboID`, `person`.`codeU` FROM abiturient.person WHERE idPerson = " + personIdMySql + ";");
                person.next();
                edboIdPerson = person.getInt(1);
                codeUPerson = person.getString(2);
                if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage("Неможливо додати заяву для персони, що не пройшла синхронизацію з ЄДБО.");
                    return json.toJson(submitStatus);
                }
                ResultSet personRequestOlympiadRS = mySqlStatement.executeQuery(""
                        + "SELECT `personspeciality`.`OlympiadID` "
                        + "FROM abiturient.personspeciality "
                        + "WHERE idPersonSpeciality = " + personSpeciality + ";");
                int idOlympiadAward = 0; // идентификатор олимпиады персоны
                int personOlympiadIdEdbo = 0; // идентификатор записи об олимпиаде персоны в ЕДБО
                String personRequestOlympiadAwardBonus = ""; // бонус за олимпиаду
                if (personRequestOlympiadRS.next()) {
                    idOlympiadAward = personRequestOlympiadRS.getInt("OlympiadID");
                }
                if (idOlympiadAward != 0) {
                    // в заявке есть олимпиада
                    // дополнительный балл
                    ResultSet olympBonus = mySqlStatement.executeQuery("SELECT OlympiadAwardBonus FROM abiturient.olympiadsawards WHERE OlympiadAwardID = " + idOlympiadAward + ";");
                    if (olympBonus.next()) {
                        personRequestOlympiadAwardBonus = olympBonus.getString(1);
                    }
                    // синхронизация списка олимпиад с ЕДБО
                    syncPersonOlympiadsEdbo(codeUPerson, personIdMySql);
                    ResultSet personOlympiadsRS = mySqlStatement.executeQuery(""
                            + "SELECT * "
                            + "FROM abiturient.personolympiad "
                            + "WHERE PersonID = " + personIdMySql + " AND OlympiadAwarID = " + idOlympiadAward + ";");
                    if (!personOlympiadsRS.next()) {
                        // такой олимпиады нет в списке персон и едбо - добавляем
                        personOlympiadIdEdbo = personSoap.personOlympiadsAwardsAdd(sessionGuid, languageId, edboIdPerson, idOlympiadAward);
                        if (personOlympiadIdEdbo == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання олімпіади  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                            return json.toJson(submitStatus);
                        } else {
                            System.out.println("Додано олімпіаду: " + personOlympiadIdEdbo);
                        }
                        personOlympiadsRS.moveToInsertRow();
                        personOlympiadsRS.updateInt("PersonID", personIdMySql);
                        personOlympiadsRS.updateInt("OlympiadAwarID", idOlympiadAward);
                        personOlympiadsRS.updateInt("edboID", personOlympiadIdEdbo);
                        personOlympiadsRS.insertRow();
                        personOlympiadsRS.moveToCurrentRow();
                    } else {
                        personOlympiadIdEdbo = personOlympiadsRS.getInt("edboID");
                    }
                    if (personOlympiadIdEdbo == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання олімпіади  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                        return json.toJson(submitStatus);
                    }

                }
                ResultSet request = mySqlStatement.executeQuery(""
                        + "SELECT * "
                        + "FROM abiturient.personspeciality "
                        + "WHERE idPersonSpeciality = " + personSpeciality + ";");
                if (request.next()) {
                    int originalDocumentsAdd = (request.getInt("isCopyEntrantDoc") == 1) ? 0 : 1;
                    int isNeedHostel = request.getInt("isNeedHostel");
                    String codeOfBusiness = "";
                    int qualificationId = request.getInt("QualificationID");
                    String courseId = Integer.toString(request.getInt("CourseID"));
//                    switch (qualificationId) {
//                        case 1:
//                            codeOfBusiness += "Б";
//                            break;
//                        case 2:
//                        case 3:
//                            codeOfBusiness += "СМ";
//                            break;
//                        case 4:
//                            codeOfBusiness += "МC";
//                            break;
//                    }
//                    codeOfBusiness += courseId + String.format("%05d", request.getInt("PersonRequestNumber"));
                    codeOfBusiness = String.format("%05d", request.getInt("RequestNumber"));
                    int idPersonEntranceType = request.getInt("EntranceTypeID");
                    int idPersonExamenationCause = request.getInt("CausalityID");
                    int idUniversityQuota1 = (request.getInt("Quota1") == 1) ? 1506 : 0;
                    int idUniversityQuota2 = (request.getInt("Quota2") == 1) ? 1681 : 0;
                    int isBudget = request.getInt("isBudget");
                    int isContract = request.getInt("isContract");
                    int idPersonEducationForm = request.getInt("EducationFormID");
                    int idPersonCourse = request.getInt("CoursedpID");
                    String personRequestCourseBonus = Float.toString(request.getFloat("CoursedpBall"));
                    int isHigherEducation = request.getInt("isHigherEducation");
                    int skipDocumentValue = request.getInt("SkipDocumentValue");
                    int idDocumentSubject1 = request.getInt("DocumentSubject1");
                    int idDocumentSubject2 = request.getInt("DocumentSubject2");
                    int idDocumentSubject3 = request.getInt("DocumentSubject3");
                    int specialityId = request.getInt("SepcialityID");
                    String universitySpecialitiesCode = "";
                    int idPersonDocument = request.getInt("EntrantDocumentID");
                    // если запись уже была добавлена, то обновляем ее поля в ЕДБО
                    if (request.getInt("edboID") != 0) {
                        if (personSoap.personRequestEdit(sessionGuid,
                                request.getInt("edboID"),
                                originalDocumentsAdd,
                                isNeedHostel,
                                codeOfBusiness,
                                isBudget,
                                isContract,
                                isHigherEducation,
                                skipDocumentValue) == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Помилка редагування заявки  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                            return json.toJson(submitStatus);
                        } else {
                            submitStatus.setError(false);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Заявку успішно відредаговано.<br />");
                            return json.toJson(submitStatus);
                        } // if - else
                    } // if
                    // иначе добавляем заявку в ЕДБО
                    System.out.println("original  " + originalDocumentsAdd);
                    System.out.println("additional Ball     " + personRequestCourseBonus);


                    if (idDocumentSubject1 != 0) {
                        // есть первый предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject1 = mySqlStatement.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject1 + ";");
                        if (subject1.next()) {
                            idDocumentSubject1 = subject1.getInt(1);
                        }
                    }
                    if (idDocumentSubject2 != 0) {
                        // есть второй предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject2 = mySqlStatement.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject2 + ";");
                        if (subject2.next()) {
                            idDocumentSubject2 = subject2.getInt(1);
                        }
                    }
                    if (idDocumentSubject3 != 0) {
                        // есть третий предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject3 = mySqlStatement.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject3 + ";");
                        if (subject3.next()) {
                            idDocumentSubject3 = subject3.getInt(1);
                        }
                    }
                    ResultSet specCodeRS = mySqlStatement.executeQuery(""
                            + "SELECT SpecialityKode "
                            + "FROM abiturient.specialities "
                            + "WHERE idSpeciality = " + specialityId + ";");
                    if (specCodeRS.next()) {
                        universitySpecialitiesCode = specCodeRS.getString(1);
                    }
                    ResultSet docCodeRs = mySqlStatement.executeQuery(""
                            + "SELECT edboID "
                            + "FROM abiturient.documents "
                            + "WHERE idDocuments = " + idPersonDocument + ";");
                    if (docCodeRs.next()) {
                        idPersonDocument = docCodeRs.getInt(1);
                    }
                    // синхронизация подготовительных курсов
                    if (idPersonCourse != 0) {
                        int personCourseIdold = idPersonCourse;
                        // 1 проверям наличие записий о курсах персоны в нашей базе
                        ArrayOfDPersonCourses coursesArray = personSoap.personCoursesGet(sessionGuid, actualDate, languageId, edboIdPerson, seasonId, universityKey);
                        List<DPersonCourses> coursesList = coursesArray.getDPersonCourses();
                        for (DPersonCourses courses : coursesList) {
                            ResultSet coursesIdRS = mySqlStatement.executeQuery("SELECT idCourseDP \n"
                                    + "FROM abiturient.coursedp\n"
                                    + "WHERE guid LIKE \"" + courses.getUniversityCourseCode() + "\";");
                            coursesIdRS.next();
                            int coursesIdLocal = coursesIdRS.getInt(1);
                            coursesIdRS.close();
                            ResultSet coursesRS = mySqlStatement.executeQuery(""
                                    + "SELECT * \n"
                                    + "FROM abiturient.personcoursesdp \n"
                                    + "WHERE \n"
                                    + "PersonID = " + personIdMySql + " \n"
                                    + "AND\n"
                                    + "guid LIKE \"" + courses.getUniversityCourseCode() + "\";");
                            if (!coursesRS.next()) {
                                // если нет то заносим
                                coursesRS.moveToInsertRow();
                                coursesRS.updateInt("PersonID", personIdMySql);
                                coursesRS.updateInt("CourseDPID", coursesIdLocal);
                                coursesRS.updateInt("edboID", courses.getIdPersonCourse());
                                coursesRS.updateString("guid", courses.getUniversityCourseCode());
                                coursesRS.insertRow();
                                coursesRS.moveToCurrentRow();
                            }
                        }
                        // 2 проверяем наличие строки соответствующими курсами у персоны
                        ResultSet coursesGuidRS = mySqlStatement.executeQuery("SELECT guid\n"
                                + "FROM `abiturient`.`coursedp` \n"
                                + "WHERE \n"
                                + "idCourseDP = " + personCourseIdold + ";");
                        coursesGuidRS.next();
                        String coursesGuid = coursesGuidRS.getString(1);
                        ResultSet coursesRS = mySqlStatement.executeQuery(""
                                + "SELECT * \n"
                                + "FROM abiturient.personcoursesdp \n"
                                + "WHERE \n"
                                + "PersonID = " + personIdMySql + " \n"
                                + "AND\n"
                                + "CourseDPID = " + personCourseIdold + ";");
                        if (coursesRS.next() && coursesRS.getInt("edboID") != 0) {
                            idPersonCourse = coursesRS.getInt("edboID");
                        } else {
                            // если не найдено, то нет и в едбо, значит обновляем
                            idPersonCourse = personSoap.personCoursesAdd(sessionGuid, languageId, edboIdPerson, coursesGuid, 0, seasonId, "");
                            coursesRS.moveToInsertRow();
                            coursesRS.updateInt("PersonID", personIdMySql);
                            coursesRS.updateInt("CourseDPID", personCourseIdold);
                            coursesRS.updateInt("edboID", idPersonCourse);
                            coursesRS.insertRow();
                            coursesRS.moveToCurrentRow();
                        }
                        if (idPersonCourse == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            System.out.println(coursesGuid);
                            submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати курси  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                            System.out.println(seasonId + " " + codeUPerson + " " + universitySpecialitiesCode + " " + idPersonEducationForm + " " + idPersonDocument);
                            return json.toJson(submitStatus);
                        }
                    }
                    System.out.println(idPersonDocument);
//                    System.out.println(idPersonExamenationCause + "\t" + idPersonEntranceType+ "\tCause: " + ((idPersonExamenationCause != 0 && idPersonEntranceType != 1) ? idPersonExamenationCause : ((idPersonEntranceType == 1) ? 0 : 100)));
                    if (personSoap.personRequestCheckCanAdd(sessionGuid, 3, codeUPerson, universitySpecialitiesCode, 0, idPersonEducationForm, idPersonDocument, 0, "") == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати заявку  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                        System.out.println(seasonId + " " + codeUPerson + " " + universitySpecialitiesCode + " " + idPersonEducationForm + " " + idPersonDocument);
                        return json.toJson(submitStatus);
                    } else {
                        System.out.println("Beeengooooooooooooooooooo");
                    }

                    int edboId = personSoap.personRequestAdd(sessionGuid, // 1
                            seasonId, // 2
                            codeUPerson, // 3
                            universitySpecialitiesCode, // 4
                            originalDocumentsAdd, //5
                            isNeedHostel, // 6
                            codeOfBusiness, // 7
                            (idPersonEntranceType != 0) ? idPersonEntranceType : 2, // 8
                            ((idPersonExamenationCause != 0 && idPersonEntranceType != 1) ? idPersonExamenationCause : ((idPersonEntranceType == 1) ? 0 : 100)), // 9
                            idUniversityQuota1, // 10
                            idUniversityQuota2, // 11
                            0, // 12
                            0, // 13
                            isBudget, // 14
                            isContract, // 15
                            idPersonEducationForm, // 16
                            idDocumentSubject1, // 17
                            idDocumentSubject2, // 18
                            idDocumentSubject3, // 19
                            idPersonCourse, // 20
                            personRequestCourseBonus, // 21
                            personOlympiadIdEdbo, //idOlympiadAward, // 22
                            personRequestOlympiadAwardBonus, // 23
                            idPersonDocument, // 24
                            0, // 25
                            "", // 26
                            isHigherEducation, // 27
                            skipDocumentValue, // 28
                            0,
                            0,
                            0);
                    if (edboId == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання заявки  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />");
                        return json.toJson(submitStatus);
                    } else {
                        mySqlStatement.executeUpdate("UPDATE `abiturient`.`personspeciality`\n"
                                + "SET\n"
                                + "`edboID` = " + edboId + "\n"
                                + "WHERE idPersonSpeciality = " + personSpeciality + ";");
                        ResultSet benefitsRS = mySqlStatement.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.personbenefits "
                                + "WHERE PersonID = " + personIdMySql + ";");
                        while (benefitsRS.next()) {
                            personSoap.personRequestBenefitsAdd(sessionGuid, actualDate, languageId, edboId, benefitsRS.getInt(1));
                        }
                        submitStatus.setError(false);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Заявку успішно додано до ЄДБО<br />");
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }
//            mySqlConnectionClose();
        } else {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка з’єднання.");
            return json.toJson(submitStatus);
        }
        return json.toJson(submitStatus);
    }

    public void editRequestsAll() {
        if (mySqlConnect() && personConnect()) {
            String sql = "SELECT * FROM abiturient.personspeciality WHERE edboID is not null;";
            try {
                ResultSet request = mySqlStatement.executeQuery(sql);
                while (request.next()) {
                    int idPersonRequest = request.getInt("edboID");
                    int originalDocumentsAdd = (request.getInt("isCopyEntrantDoc") == 1) ? 0 : 1;
                    int isNeedHostel = request.getInt("isNeedHostel");
                    String codeOfBusiness = String.format("%05d", request.getInt("RequestNumber"));
                    int isBudget = request.getInt("isBudget");
                    int isContract = request.getInt("isContract");
                    int isHigherEducation = request.getInt("isHigherEducation");
                    int skipDocumentValue = request.getInt("SkipDocumentValue");
                    if (personSoap.personRequestEdit(sessionGuid,
                            idPersonRequest,
                            originalDocumentsAdd,
                            isNeedHostel,
                            codeOfBusiness,
                            isBudget,
                            isContract,
                            isHigherEducation,
                            skipDocumentValue) == 0) {
                        System.out.println(personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
//            mySqlConnectionClose();
        }
    }

    public void syncRequestsAll() {
        if (mySqlConnect()) {
            String sql = "SELECT * FROM abiturient.personspeciality WHERE edboID is null;";
            try {
                ResultSet request = mySqlStatement.executeQuery(sql);
                while (request.next()) {
                    int idPersonSpeciality = request.getInt("idPersonSpeciality");
                    int idPerson = request.getInt("PersonID");
                    System.out.println("Персона:   " + idPerson + "Заявка: " + idPersonSpeciality);
                    System.out.println(addPersonRequestEdbo(idPerson, idPersonSpeciality));
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void coursesUpdateEdbo() {
        if (mySqlConnect() && guidesConnect()) {
            ArrayOfDUniversityCourses coursesArray = guidesSoap.universityCoursesGet(sessionGuid, languageId, actualDate, universityKey, seasonId);
            List<DUniversityCourses> coursesList = coursesArray.getDUniversityCourses();
            for (DUniversityCourses course : coursesList) {
                System.out.println(course.getIdUniversityCourse() + " " + course.getUniversityCourseName() + "   " + course.getUniversityCourseCode());
                try {
                    mySqlStatement.executeUpdate("UPDATE `abiturient`.`coursedp`\n"
                            + "SET\n"
                            + "guid = '" + course.getUniversityCourseCode() + "'\n"
                            + "WHERE idCourseDP = " + course.getIdUniversityCourse() + ";");


                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Добавление льготы медалистам в базе данных ЕДБО
     */
    public void medalsUpdateEdbo() {
        if (personConnect() && mySqlConnect()) {
            String sql = "select `personspeciality`.`PersonID` AS `idPersonMysql`, \n"
                    + "`person`.`edboID` AS `idPersonEdbo`,\n"
                    + "`personspeciality`.`edboID` as `idPersonRequest`, \n"
                    + "`personbenefits`.`edboID` AS `idPersonBenefit`\n"
                    + "from\n"
                    + "`abiturient`.`personspeciality` join `abiturient`.`personbenefits`\n"
                    + "on `personspeciality`.`PersonID` = `personbenefits`.`PersonID` \n"
                    + "join `abiturient`.`person`\n"
                    + "on `personspeciality`.`PersonID` = `person`.`idPerson`\n"
                    + "where\n"
                    + "`personbenefits`.`BenefitID` = 39;";
            try {
                ResultSet requestWithMedals = mySqlStatement.executeQuery(sql);
//                ArrayList<Integer> personWithMedal = new ArrayList<Integer>();
//                while (requestWithMedals.next()) {
//                    int idPersonMySql = requestWithMedals.getInt("idPersonMysql");
//                    personWithMedal.add(idPersonMySql);
//                }
//                for (int iPerson: personWithMedal){
//                    addPersonBenefits(iPerson);
//                }
//                requestWithMedals = mySqlStatement.executeQuery(sql);
                while (requestWithMedals.next()) {
                    int idPersonMySql = requestWithMedals.getInt("idPersonMysql");
                    int idPersonEdbo = requestWithMedals.getInt("idPersonEdbo");
                    int idPersonRequest = requestWithMedals.getInt("idPersonRequest");
                    int idPersonBenefit = requestWithMedals.getInt("idPersonBenefit");
                    if (idPersonBenefit != 0) {

                        int result = personSoap.personRequestBenefitsAdd(sessionGuid, actualDate, languageId, idPersonRequest, idPersonBenefit);
                        if (result == 0) {
                            System.out.println(idPersonMySql + ": ЄДБО " + idPersonEdbo + ": заявка № " + idPersonRequest + ": Помилка додавання пільги доя заяки  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        } else {
                            System.out.println(idPersonMySql + ": ЄДБО " + idPersonEdbo + ": заявка № " + idPersonRequest + ": Льгота про медаль додана до заявки");
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Добавление льгот в заявки в ЕДБО
     */
    public void requestBenefitsAddEdbo() {
        if (personConnect() && mySqlConnect()) {
            try {
                // синхронизация списка льгот
                ResultSet benefits = mySqlStatement.executeQuery("SELECT * FROM abiturient.personbenefits where edboID is null;");
                while (benefits.next()) {
                    String status = addPersonBenefits(benefits.getInt("PersonID"));
                    System.out.println("Пільга " + benefits.getInt("idPersonBenefits") + ": " + status);
                } // while
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            } // try - catch
            try {
                ResultSet requestBenefits = mySqlStatement.executeQuery("select `personspeciality`.`idPersonSpeciality` AS `idPersonSpeciality`, `personspeciality`.`edboID` AS `idPersonRequest`, `personbenefits`.`edboID` AS `idPersonBenefit`, `personbenefits`.`BenefitID` AS `idBenefit`, `personspeciality`.`CoursedpID` AS `idCourseDP`\n"
                        + "from `abiturient`.`personspeciality` join `abiturient`.`personbenefits`\n"
                        + "    on `personspeciality`.`PersonID` = `personbenefits`.`PersonID`;");
                while (requestBenefits.next()) {
                    int idBenefit = requestBenefits.getInt("idBenefit");
                    int idCourseDP = requestBenefits.getInt("idCourseDP");
                    int idPersonSpeciality = requestBenefits.getInt("idPersonSpeciality");
                    int idPersonRequest = requestBenefits.getInt("idPersonRequest");
                    int idPersonBenefit = requestBenefits.getInt("idPersonBenefit");
                    if (idBenefit != 41 || (idBenefit == 41 && idCourseDP != 0)) {
                        int result = personSoap.personRequestBenefitsAdd(sessionGuid, actualDate, languageId, idPersonRequest, idPersonBenefit);
                        if (result == 0) {
                            System.out.println(idPersonSpeciality + ": ЄДБО  заявка № " + idPersonRequest + ": Помилка додавання пільги до заявки  :  " + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        } else {
                            System.out.println(idPersonSpeciality + ": ЄДБО  заявка № " + idPersonRequest + ": Пільга додана до заявки");
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } // if (personConnect() && mySqlConnect())
    }

    /**
     * Получить список допустимых статусов заявок персоны из ЕДБО
     */
    public void getRequestsStatuses() {
        if (personConnect()) {
            ArrayOfDPersonRequestStatusTypes statusArray = personSoap.personRequestStatusTypesGet(sessionGuid, actualDate, languageId);
            List<DPersonRequestStatusTypes> statusList = statusArray.getDPersonRequestStatusTypes();
            for (DPersonRequestStatusTypes status : statusList) {
                System.out.println(status.getIdPersonRequestStatusType() + "\t" + status.getPersonRequestStatusTypeName()
                        + "\t" + status.getPersonRequestStatusTypeDescription() + "\t" + status.getPersonRequestStatusCode());
            }
        }
    }

    /**
     * Изменить статус заявок в базе ЕДБО
     * <p>Метод выбирает все синхронизированные заявки в базе MySQL. Затем для
     * каждой заявки проверяется ее статус в базе ЕДБО. Если статус
     * соответствовал параметру fromStatus то он меняется на toStatus.</p>
     *
     * @param fromStatus Исходное значение статуса заяки
     * @param toStatus Новое значение статуса заяки
     * @return Описание попытки
     */
    public String changeRequestStatus(int fromStatus, int toStatus) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);

        if (mySqlConnect() && personConnect()) {
            String sql = "SELECT * FROM abiturient.personspeciality where edboID is not null;";
            try {
                ResultSet request = mySqlStatement.executeQuery(sql);
                while (request.next()) {

                    int idPersonRequest = request.getInt("edboID");
//                    int isBudget = request.getInt("isBudget");
//                    int isContract = request.getInt("isBudget");
                    ArrayOfDPersonRequestsStatuses requestStatusArray = personSoap.personRequestsStatusesGet(sessionGuid, languageId, idPersonRequest);
                    List<DPersonRequestsStatuses> requestStatusList = requestStatusArray.getDPersonRequestsStatuses();
                    DPersonRequestsStatuses lastStatus = requestStatusList.get(0);
                    int idPersonRequestStatusType = lastStatus.getIdPersonRequestStatusType();
                    int idUniversityEntrantWave = lastStatus.getIdUniversityEntrantWave();
                    if (idPersonRequestStatusType == fromStatus) {
                        if (personSoap.personRequestsStatusChange(
                                sessionGuid,
                                idPersonRequest,
                                toStatus,
                                "",
                                idUniversityEntrantWave,
                                -1, //isBudget, 
                                -1) == 0) {//isContract) == 0) {
//                                submitStatus.setMessage(submitStatus.getMessage() + idPersonRequest + ":\tПомилка зміни статусу заявки:\t" + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />\n");
                            System.out.println(idPersonRequest + ":\tПомилка зміни статусу заявки:\t" + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        } else {
//                                submitStatus.setMessage(submitStatus.getMessage() + idPersonRequest + ":\tCтатус заяки змінено<br />\n");
                            System.out.println(idPersonRequest + ":\tCтатус заяки змінено");
                            request.updateInt("StatusID", toStatus);
                            request.updateRow();
                        } // if - else
                    } // if
//                    for (DPersonRequestsStatuses requestStatus : requestStatusList) {
//                        int idPersonRequestStatusType = requestStatus.getIdPersonRequestStatusType();
////                        int idPersonRequestStatus = requestStatus.getIdPersonRequestStatus();
//                        int idUniversityEntrantWave = requestStatus.getIdUniversityEntrantWave();
//                        if (idPersonRequestStatusType == fromStatus) {
//                            if (personSoap.personRequestsStatusChange(
//                                    sessionGuid,
//                                    idPersonRequest,
//                                    toStatus,
//                                    "",
//                                    idUniversityEntrantWave,
//                                    -1, //isBudget, 
//                                    -1) == 0) {//isContract) == 0) {
////                                submitStatus.setMessage(submitStatus.getMessage() + idPersonRequest + ":\tПомилка зміни статусу заявки:\t" + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription() + "<br />\n");
//                                System.out.println(idPersonRequest + ":\tПомилка зміни статусу заявки:\t" + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
//                            } else {
////                                submitStatus.setMessage(submitStatus.getMessage() + idPersonRequest + ":\tCтатус заяки змінено<br />\n");
//                                System.out.println(idPersonRequest + ":\tCтатус заяки змінено");
//                                request.updateInt("StatusID", toStatus);
//                                request.updateRow();
//                            } // else
//                        } // if
//                    } // for

                } // while
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            } // try - catch
        } else {
            submitStatus.setError(true);
            submitStatus.setMessage("Помилка з’єднання.");
        } // if - else
//        System.out.println(submitStatus.getMessage());
        return json.toJson(submitStatus);
    }

    /**
     * Синхронизация статусов всех зоявок с их статусами в ЕДБО
     */
    public void syncRequestsStatuses() {
        if (mySqlConnect() && personConnect()) {
            String sql = "SELECT * FROM abiturient.personspeciality where edboID is not null;";
            try {
                ResultSet request = mySqlStatement.executeQuery(sql);
                while (request.next()) {
                    int currentPersonStatusId = request.getInt("StatusID");
                    int idPersonRequest = request.getInt("edboID");
                    ArrayOfDPersonRequestsStatuses requestStatusArray = personSoap.personRequestsStatusesGet(sessionGuid, languageId, idPersonRequest);
                    List<DPersonRequestsStatuses> requestStatusList = requestStatusArray.getDPersonRequestsStatuses();
                    DPersonRequestsStatuses lastStatus = requestStatusList.get(0);
                    int idPersonRequestStatusType = lastStatus.getIdPersonRequestStatusType();
                    if (idPersonRequestStatusType == currentPersonStatusId) {
                        System.out.println("Заявка " + idPersonRequest + ": статус актуальний.");
                    } else {
                        request.updateInt("StatusID", idPersonRequestStatusType);
                        request.updateRow();
                        System.out.println("Заявка " + idPersonRequest + ": в базі MySQL статус змінено з " + currentPersonStatusId + " на " + idPersonRequestStatusType);
                    } // if - else
                } // while
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            } // try - catch
        } else {
            System.out.println("Помилка з’єднання.");
        } // if - else
    } // syncRequestsStatuses()

    public void olympiadsAwardsGet() {
        if (guidesConnect() && mySqlConnect()) {
            ArrayOfDOlympiadsAwards olympiadsAwardArray = guidesSoap.olympiadsAwardsGet(sessionGuid, languageId, actualDate, seasonId);
            List<DOlympiadsAwards> olympiadsAwardsList = olympiadsAwardArray.getDOlympiadsAwards();
            for (DOlympiadsAwards award : olympiadsAwardsList) {
                System.out.println(award.getIdOlympiadAward() + "\t"
                        + award.getOlympiadAwardName() + '\t'
                        + award.getOlympiadAwardBonus() + '\t'
                        + award.getIdOlimpiad() + '\t'
                        + award.getOlimpiadName());
                String sql = "INSERT INTO `abiturient`.`olympiadsawards`\n"
                        + "(`idOlimpiad`,\n"
                        + "`OlimpiadName`,\n"
                        + "`OlympiadAwardID`,\n"
                        + "`OlympiadAwardName`,\n"
                        + "`OlympiadAwardBonus`)\n"
                        + "VALUES\n"
                        + "(\n"
                        + award.getIdOlimpiad() + ",\n"
                        + "'" + award.getOlimpiadName() + "',\n"
                        + award.getIdOlympiadAward() + ",\n"
                        + "'" + award.getOlympiadAwardName() + "',\n"
                        + award.getOlympiadAwardBonus() + "\n"
                        + ");";
                try {
                    mySqlStatement.executeUpdate(sql);
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public String getSessionGuid() {
        return sessionGuid;
    }

    public String getActualDate() {
        return actualDate;
    }

    public int getLanguageId() {
        return languageId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public String getUniversityKey() {
        return universityKey;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }
}