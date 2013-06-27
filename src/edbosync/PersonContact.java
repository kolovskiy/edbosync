/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edbosync;

/**
 * Контакт, соответствующий персоне
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 */
public class PersonContact {

    /**
     * Идентификатор контакта в базе EDBO
     */
    private int id_Contact;
    /**
     * Идентификатор типа контакта персоны (email, дом. телефон и т.д.)
     */
    private int id_ContactType;
    /**
     * Значение контакта (например, "abc@defg.com")
     */
    private String value;
    /**
     * Флаг указывает на то, что контакт является основным
     */
    private int isDefault;

    /**
     * Получить идентификатор контакта в ЕДБО
     *
     * @return Идентификатор контакта в ЕДБО
     */
    public int getId_Contact() {
        return id_Contact;
    }

    /**
     * Установить значение идентификатора контакта в ЕДБО
     *
     * @param id_Contact Новое значение идентификатора контакта в ЕДБО
     */
    public void setId_Contact(int id_Contact) {
        this.id_Contact = id_Contact;
    }

    /**
     * Получить значение идентификатора типа контакта
     *
     * @return Значение идентификатора типа контакта
     */
    public int getId_ContactType() {
        return id_ContactType;
    }

    /**
     * Установить значение идентификатора типа контакта
     *
     * @param id_ContactType Новое значение идентификатора типа контакта
     */
    public void setId_ContactType(int id_ContactType) {
        this.id_ContactType = id_ContactType;
    }

    /**
     * Получить значение контакта
     *
     * @return Значение контакта
     */
    public String getValue() {
        return value;
    }

    /**
     * Установить значение контакта
     *
     * @param value Новое значение контакта
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Получить значение флага об использовании данного контакта в качестве
     * основного
     *
     * @return Значение флага использования данного контакта в качестве
     * основного
     */
    public int getIsDefault() {
        return isDefault;
    }

    /**
     * Установить значение флага об использовании данного контакта в качестве
     * основного
     *
     * @param isDefault Новое значение флага использования данного контакта в
     * качестве основного
     */
    public void setIsDefault(int isDefault) {
        this.isDefault = isDefault;
    }
}
