package edbosync;

import java.util.Calendar;
import java.util.ArrayList;

/**
 * Персона: фамилия, имя, отчество, дата рождения и др.
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 * @see edbosync.PersonContact
 */
public class Person {

    /**
     * Фамилия
     */
    private String lastName;
    /**
     * Имя
     */
    private String firstName;
    /**
     * Отчество
     */
    private String middleName;
    /**
     * Идентификатор пола персоны
     */
    private int id_PersonSex;
    /**
     * Дата рождения
     */
    private java.util.Calendar birthday;
    /**
     * Признак резидента: 0 - нерезидент, 1 - резидент
     */
    private int resident;
    /**
     * Код персоны в ЕДБО
     */
    private String personCodeU;
    /**
     * Идентификатор персоны в базе ЕДБО
     */
    private int id_Person = -1;
    /**
     * Идентификатор кода KOATUU уровня 1
     */
    private int id_KoatuuCodeL1 = -1;
    /**
     * Идентификатор кода KOATUU уровня 2
     */
    private int id_KoatuuCodeL2 = -1;
    /**
     * Идентификатор кода KOATUU уровня 3
     */
    private int id_KoatuuCodeL3 = -1;
    /**
     * Идентификатор типа улицы
     */
    private int id_StreetType = 0;
    /**
     * Адрес
     */
    private String address;
    /**
     * Номер дома
     */
    private String homeNumber;
    /**
     * Почтовый индекс
     */
    private String postIndex;
    // Контакты
//    private ArrayList<PersonContact> contacts = new ArrayList<>(); //!< Список констактов персоны

    /**
     * Получить фамилию
     *
     * @return фамилия
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Установить фамилию
     *
     * @param lastName новое значение фамилии
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Получить имя
     *
     * @return имя
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Установить имя
     *
     * @param firstName новое значение имени
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Получить отчество
     *
     * @return отчество
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Установить отчество
     *
     * @param middleName новое значение отчества
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Получить идентификатор пола
     *
     * @return идентификатор пола
     */
    public int getId_PersonSex() {
        return id_PersonSex;
    }

    /**
     * Установить идентификатор пола
     *
     * @param id_PersonSex новое значение идентификатора пола
     */
    public void setId_PersonSex(int id_PersonSex) {
        this.id_PersonSex = id_PersonSex;
    }

    /**
     * Получить дату рождения
     *
     * @return дата рождения
     */
    public Calendar getBirthday() {
        return birthday;
    }

    /**
     * Установить дату рождения
     *
     * @param birthday новое значение даты рождения
     */
    public void setBirthday(Calendar birthday) {
        this.birthday = birthday;
    }

    /**
     * Получить признак резидента
     *
     * @return признак резидента
     */
    public int getResident() {
        return resident;
    }

    /**
     * Установить новое значение признака резидента
     *
     * @param resident новое значение признака резидента
     */
    public void setResident(int resident) {
        this.resident = resident;
    }

    /**
     * Получить код персоны в базе ЕДБО
     *
     * @return код персоны в базе ЕДБО
     */
    public String getPersonCodeU() {
        return personCodeU;
    }

    /**
     * Установить код персоны в базе ЕДБО
     *
     * @param personCodeU новое значение кода персоны в базе ЕДБО
     */
    public void setPersonCodeU(String personCodeU) {
        this.personCodeU = personCodeU;
    }

    /**
     * Получить значение идентификатора персоны в базе ЕДБО
     *
     * @return значение идентификатора персоны в базе ЕДБО
     */
    public int getId_Person() {
        return id_Person;
    }

    /**
     * Установить значение идентификатора персоны в базе ЕДБО
     *
     * @param id_Person новое значение идентификатора персоны в базе ЕДБО
     */
    public void setId_Person(int id_Person) {
        this.id_Person = id_Person;
    }

    /**
     * Получить значение идентификатора KOTUU уровня 1
     *
     * @return значение идентификатора KOTUU уровня 1
     */
    public int getId_KoatuuCodeL1() {
        return id_KoatuuCodeL1;
    }

    /**
     * Установить значение идентификатора KOTUU уровня 1
     *
     * @param id_KoatuuCodeL1 новое значение идентификатора KOTUU уровня 1
     */
    public void setId_KoatuuCodeL1(int id_KoatuuCodeL1) {
        this.id_KoatuuCodeL1 = id_KoatuuCodeL1;
    }

    /**
     * Получить значение идентификатора KOTUU уровня 2
     *
     * @return значение идентификатора KOTUU уровня 2 (-1, если не определено)
     */
    public int getId_KoatuuCodeL2() {
        return id_KoatuuCodeL2;
    }

    /**
     * Установить значение идентификатора KOTUU уровня 2
     *
     * @param id_KoatuuCodeL2 новое значение идентификатора KOTUU уровня 2
     */
    public void setId_KoatuuCodeL2(int id_KoatuuCodeL2) {
        this.id_KoatuuCodeL2 = id_KoatuuCodeL2;
    }

    /**
     * Получить значение идентификатора KOTUU уровня 3
     *
     * @return значение идентификатора KOTUU уровня 3 (-1, если не определено)
     */
    public int getId_KoatuuCodeL3() {
        return id_KoatuuCodeL3;
    }

    /**
     * Установить значение идентификатора KOTUU уровня 3
     *
     * @param id_KoatuuCodeL3 новое значение идентификатора KOTUU уровня 3
     */
    public void setId_KoatuuCodeL3(int id_KoatuuCodeL3) {
        this.id_KoatuuCodeL3 = id_KoatuuCodeL3;
    }

    /**
     * Получить идентификатор типа улицы
     *
     * @return идентификатор типа улицы
     */
    public int getId_StreetType() {
        return id_StreetType;
    }

    /**
     * Установить идентификатор типа
     *
     * @param id_StreetType новое значение идентификатора типа улицы
     */
    public void setId_StreetType(int id_StreetType) {
        this.id_StreetType = id_StreetType;
    }

    /**
     * Получить название улицы
     *
     * @return название улицы
     */
    public String getAddress() {
        return address;
    }

    /**
     * Установить название улицы
     *
     * @param address новое значение названия улицы
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Получить номер дома
     *
     * @return номер дома
     */
    public String getHomeNumber() {
        return homeNumber;
    }

    /**
     * Установить номер дома
     *
     * @param homeNumber новое значение номера дома
     */
    public void setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
    }

    /**
     * Получить почтовый индекс
     *
     * @return почтовый индекс
     */
    public String getPostIndex() {
        return postIndex;
    }

    /**
     * Установить новое значение почтового индекса
     *
     * @param postIndex новое значение почтового индекса
     */
    public void setPostIndex(String postIndex) {
        this.postIndex = postIndex;
    }
    /**
     * Получить список контактов персоны
     *
     * @return список контактов персоны
     */
//    public ArrayList<PersonContact> getContacts() {
//        return contacts;
//    }
//
//    /**
//     * Установить список контактов персоны
//     *
//     * @param contacts новый список контактов персоны
//     */
//    public void setContacts(ArrayList<PersonContact> contacts) {
//        this.contacts = contacts;
//    }
}
