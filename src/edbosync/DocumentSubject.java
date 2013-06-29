package edbosync;

/**
 * Предмет документа (актуален для документов "аттестат" и "сертификат ЗНО")
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 */
public class DocumentSubject {

    /**
     * Идентификатор предмета (MySQL + ЕДБО)
     */
    private int id_Subject;
    /**
     * Балл по предмету
     */
    private double subjectValue;
    /**
     * Идентификатор записи о предмете для документа (ЕДБО)
     */
    private int id_DocumentSubject;
    /**
     * Идентификатор документа в базе MySQL
     */
    private int documentId;
    /**
     * Название предмета
     */
    private String subjectName = "";

    /**
     * Получить значение идентификатора предмета
     *
     * @return значение идентификатора предмета
     */
    public int getId_Subject() {
        return id_Subject;
    }

    /**
     * Установить значение идентификатора предмета
     *
     * @param id_Subject новое значение идентификатора предмета
     */
    public void setId_Subject(int id_Subject) {
        this.id_Subject = id_Subject;
    }

    /**
     * Получить значение оценки по предмету
     *
     * @return значение оценки по предмету
     */
    public double getSubjectValue() {
        return subjectValue;
    }

    /**
     * Установить значение оценки по предмету
     *
     * @param subjectValue новое значение оценки по предмету
     */
    public void setSubjectValue(double subjectValue) {
        this.subjectValue = subjectValue;
    }

    /**
     * Получить значение идентификатора записи о предмете для документа
     *
     * @return Значение идентификатора записи о предмете для документа
     */
    public int getId_DocumentSubject() {
        return id_DocumentSubject;
    }

    /**
     * Установить значение идентификатора записи о предмете для документа
     *
     * @param id_DocumentSubject Новое значение идентификатора записи о предмете
     * для документа
     */
    public void setId_DocumentSubject(int id_DocumentSubject) {
        this.id_DocumentSubject = id_DocumentSubject;
    }

    /**
     * Получить значение идентификатора документа в базе MySQL
     *
     * @return Значение идентификатора документа в базе MySQL
     */
    public int getDocumentId() {
        return documentId;
    }

    /**
     * Установить значение идентификатора документа в базе MySQL
     *
     * @param documentId Новое значение идентификатора документа в базе MySQL
     */
    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    /**
     * Получить название предмета
     *
     * @return Название предмета
     */
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * Установить название предмета
     *
     * @param subjectName Новое название предмета
     */
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
