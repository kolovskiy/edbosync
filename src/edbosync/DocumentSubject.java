package edbosync;

/**
 * Предмет документа (актуален для документов "аттестат" и "сертификат ЗНО")
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 */
public class DocumentSubject {

    /**
     * Идентификатор предмета
     */
    private int id_Subject;
    /**
     * Балл по предмету
     */
    private double subjectValue;

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
}
