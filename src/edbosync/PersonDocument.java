package edbosync;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.ArrayList;

/**
 * Документ персоны
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 * @see edbosync.DocumentSubject
 */
public class PersonDocument {

    /**
     * Идентификатор типа документа
     */
    private int id_Type;
    /**
     * Идентификатор документа в базе EDBO
     */
    private int id_Document;
    /**
     * Серия документа
     */
    private String series;
    /**
     * Номер документа
     */
    private String number;
    /**
     * Дата выдачи документа
     */
    private Calendar dateGet;
    /**
     * ЗНО-пин (-1, если тип документа отличается от "сертификат ЗНО")
     */
    private int znoPin = -1;
    /**
     * Балл аттестата (-1, если не установлен для данного типа документа)
     */
    private BigDecimal attestatValue = new BigDecimal(-1);
    /**
     * Название организации выдавшей документ
     */
    private String issued;
    /**
     * Список предметов документа
     */
    private ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>(); //!< список предметов с документами (формируется, если документ - сертификат ЗНО)

    /**
     * Получить значение идентификатора типа документа
     *
     * @return значение идентификатора типа документа
     */
    public int getId_Type() {
        return id_Type;
    }

    /**
     * Установить значение идентификатора типа документа
     *
     * @param id_Type новое значение идентификатора типа документа
     */
    public void setId_Type(int id_Type) {
        this.id_Type = id_Type;
    }

    /**
     * Получить значение идентификатора документа в базе ЕДБО
     *
     * @return значение идентификатора документа в базе ЕДБО
     */
    public int getId_Document() {
        return id_Document;
    }

    /**
     * Установить значение идентификатора документа в базе ЕДБО
     *
     * @param id_Document новое значение идентификатора документа в базе ЕДБО
     */
    public void setId_Document(int id_Document) {
        this.id_Document = id_Document;
    }

    /**
     * Получить серию документа
     *
     * @return серия документа
     */
    public String getSeries() {
        return series;
    }

    /**
     * Установить серию документа
     *
     * @param series новое значение серии документа
     */
    public void setSeries(String series) {
        this.series = series;
    }

    /**
     * Получить номер документа
     *
     * @return номер документа
     */
    public String getNumber() {
        return number;
    }

    /**
     * Установить номер документа
     *
     * @param number новый номер документа
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * Получить дату выдачи документа
     *
     * @return дата выдачи документа
     */
    public Calendar getDateGet() {
        return dateGet;
    }

    /**
     * Установить дату выдачи документа
     *
     * @param dateGet новое значение даты выдачи документа
     */
    public void setDateGet(Calendar dateGet) {
        this.dateGet = dateGet;
    }

    /**
     * Получить значение ПИНа ЗНО (неравно -1, если тип документа - "Сертификат
     * ЗНО")
     *
     * @return значение ПИНа ЗНО (неравно -1, если тип документа - "Сертификат
     * ЗНО")
     */
    public int getZnoPin() {
        return znoPin;
    }

    /**
     * Установить значение ПИНа ЗНО (неравно -1, если тип документа -
     * "Сертификат ЗНО")
     *
     * @param znoPin новое значение ПИНа ЗНО (неравно -1, если тип документа -
     * "Сертификат ЗНО")
     */
    public void setZnoPin(int znoPin) {
        this.znoPin = znoPin;
    }

    /**
     * Получить балл аттестата (неравно -1, если тип документа - "аттестат")
     *
     * @return балл аттестата (неравно -1, если тип документа - "аттестат")
     */
    public BigDecimal getAttestatValue() {
        return attestatValue;
    }

    /**
     * Установить балл аттестата (неравно -1, если тип документа - "аттестат")
     *
     * @param attestatValue новый балл аттестата (неравно -1, если тип документа
     * - "аттестат")
     */
    public void setAttestatValue(BigDecimal attestatValue) {
        this.attestatValue = attestatValue;
    }

    /**
     * Получить название организации, выдавшей документ
     *
     * @return название организации, выдавшей документ
     */
    public String getIssued() {
        return issued;
    }

    /**
     * Установить название организации, выдавшей документ
     *
     * @param issued новое название организации, выдавшей документ
     */
    public void setIssued(String issued) {
        this.issued = issued;
    }

    /**
     * Получить список предметов документа
     *
     * @return список предметов документа
     * @see edbosync.DocumentSubject
     */
    public ArrayList<DocumentSubject> getSubjects() {
        return subjects;
    }

    /**
     * Установить список предметов документа
     *
     * @param subjects новый список предметов документа
     * @see edbosync.DocumentSubject
     */
    public void setSubjects(ArrayList<DocumentSubject> subjects) {
        this.subjects = subjects;
    }
}
