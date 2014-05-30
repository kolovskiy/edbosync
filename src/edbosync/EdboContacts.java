package edbosync;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import ua.edboservice.ArrayOfDPersonContacts;
import ua.edboservice.DPersonContacts;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для обработки контактов в ЕДБО
 *
 * @author Сергей Чопоров
 * @version 1.0.0
 */
public class EdboContacts {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();

    /**
     * Получить контакты персоны из базы ЕДБО
     *
     * @param personCodeU Код U персоны
     * @return Массив контактов в формате json
     */
    public String load(String personCodeU) {
        EDBOPersonSoap soap = edbo.getSoap();
        Gson json = new Gson();
        ArrayList<PersonContact> contacts = new ArrayList<PersonContact>();
        ArrayOfDPersonContacts contactsArray = soap.personContactsGet(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), personCodeU, 0);
        List<DPersonContacts> contactsList = contactsArray.getDPersonContacts();
        for (DPersonContacts dContact : contactsList) {
            PersonContact contact = new PersonContact();
            contact.setId_Contact(dContact.getIdPersonContact());
            contact.setId_ContactType(dContact.getIdPersonContactType());
            contact.setIsDefault(dContact.getDefaull());
            contact.setValue(dContact.getValue());

            contacts.add(contact);
        }
        return json.toJson(contacts);
    }
}
