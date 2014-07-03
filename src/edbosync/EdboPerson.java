package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonAddresses2;
import ua.edboservice.ArrayOfDPersonsFind;
import ua.edboservice.ArrayOfDPersonsFind2;
import ua.edboservice.DPersonAddresses2;
import ua.edboservice.DPersonsFind;
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
     * Поиск в базе ЕДБО по серии и номеру документа
     *
     * @param series Серия документа
     * @param number Номер документа
     * @return Сведения о персоне в формате json
     */
    public String find(String series, String number) {
        EDBOPersonSoap soap = edbo.getSoap();
        ArrayOfDPersonsFind2 personArray = soap.personsFind2(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), "*", series, number, "", 1, "", "");
        return processArrayOfDPersonsFind(personArray);
    }

    /**
     * Поиск в базе ЕДБО по маске ФИО
     *
     * @param fio Маска ФИО
     * @return Сведения о персоне в формате json
     */
    public String find(String fio) {
        EDBOPersonSoap soap = edbo.getSoap();
        ArrayOfDPersonsFind2 personArray = soap.personsFind2(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), fio.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"), "", "", "", 1, "", "");
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
        EDBOPersonSoap soap = edbo.getSoap();
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
                ArrayOfDPersonAddresses2 adressesArray = soap.personAddressesGet2(edbo.sessionGuid, edbo.actualDate, edbo.languageId, dPerson.getPersonCodeU(), 0);
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

}
