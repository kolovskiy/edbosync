package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonBenefits2;
import ua.edboservice.DPersonBenefits2;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс дляя обработки льгот персоны в ЕДБО
 *
 * @author Сергей Чопоров
 */
public class EdboBenefits {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();
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
     * Получить сведения о льготах персоны из ЕДБО
     *
     * @param personId Идентификатор персоны в ЕДБО
     * @return Список льгот персоны в формате json
     * @see PersonBenefit
     */
    public String load(int personId) {
        ArrayList<PersonBenefit> personBenefits = new ArrayList<PersonBenefit>();
        Gson json = new Gson();
        ArrayOfDPersonBenefits2 benefitsArray = soap.personBenefitsGet2(sessionGuid, actualDate, languageId, personId);
        if (benefitsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonBenefits2> benefitsList = benefitsArray.getDPersonBenefits2();
        for (DPersonBenefits2 dBenefit : benefitsList) {
            PersonBenefit benefit = new PersonBenefit();
            benefit.setId_Benefit(dBenefit.getIdBenefit());
            benefit.setId_PersonBenefit(dBenefit.getIdPersonBenefit());
            benefit.setBenefitsDocumentSeries(dBenefit.getBenefitsDocumentSeries());
            benefit.setBenefitsDocumentNumber(dBenefit.getBenefitsDocumentNumber());
            benefit.setBenefitsDocumentIssued(dBenefit.getBenefitsDocumentIssued());
            personBenefits.add(benefit);
        }
        return json.toJson(personBenefits);
    }

    /**
     * Синхронизировать льготы персоны
     *
     * @param personIdMysql Идентификатор персоны в базе данных
     * @return Статус попытки добавить данные в формате json
     * @see SubmitStatus
     */
    public String sync(int personIdMysql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        DataBaseConnector dbc = new DataBaseConnector();
        int edboIdPerson = 0;
        String codeUPerson = "";
        ResultSet person = dbc.executeQuery("SELECT `person`.`edboID`, `person`.`codeU` FROM person WHERE idPerson = " + personIdMysql + ";");
        try {
            if (person.next()) {
                edboIdPerson = person.getInt(1);
                codeUPerson = person.getString(2);
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboBenefits.class.getName()).log(Level.SEVERE, null, ex);
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка SQL: " + ex.getLocalizedMessage());
            return json.toJson(submitStatus);
        }
        if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Неможливо синхронізувати льготи персони, яка не пройшла синхронизацію з ЄДБО.");
            return json.toJson(submitStatus);
        }
        // БД ----> ЕДБО
        ResultSet personBenefits = dbc.executeQuery("SELECT * FROM personbenefits WHERE PersonID = "
                + personIdMysql + " and edboID is null;");
        try {
            while (personBenefits.next()) {
                int benefitId = personBenefits.getInt("BenefitID");
                String series = personBenefits.getString("Series");
                String number = personBenefits.getString("Numbers");
                String issued = personBenefits.getString("Issued");
                int result = soap.personBenefitsAdd2(sessionGuid, actualDate, languageId, edboIdPerson, benefitId, series, number, issued);
                if (result == 0) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання пільги  :  " + edbo.processErrors() + "<br />");
                } else {
                    personBenefits.updateInt("edboID", result);
                    personBenefits.updateRow();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboBenefits.class.getName()).log(Level.SEVERE, null, ex);
        }
        // ЕДБО ----> БД
        ArrayOfDPersonBenefits2 benefitsArray = soap.personBenefitsGet2(sessionGuid, actualDate, languageId, edboIdPerson);
        if (benefitsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonBenefits2> benefitsList = benefitsArray.getDPersonBenefits2();
        for (DPersonBenefits2 dPersonBenefits : benefitsList) {
            ResultSet benefitMysql = dbc.executeQuery("SELECT * "
                    + "FROM abiturient.personbenefits "
                    + "WHERE PersonID = " + personIdMysql + " AND BenefitID = " + dPersonBenefits.getIdBenefit() + ";");
            try {
                if (benefitMysql.next()) {
                    // льгота найдена в БД обновляем ее идентификатор
                    benefitMysql.updateInt("edboID", dPersonBenefits.getIdPersonBenefit());
                    benefitMysql.updateRow();
                } else {
                    // неайдена: добавляем
                    benefitMysql.moveToInsertRow();
                    benefitMysql.updateInt("PersonID", personIdMysql);
                    benefitMysql.updateInt("BenefitID", dPersonBenefits.getIdBenefit());
                    benefitMysql.updateInt("edboID", dPersonBenefits.getIdPersonBenefit());
                    benefitMysql.updateString("Series", dPersonBenefits.getBenefitsDocumentSeries());
                    benefitMysql.updateString("Numbers", dPersonBenefits.getBenefitsDocumentNumber());
                    benefitMysql.updateString("Issued", dPersonBenefits.getBenefitsDocumentIssued());
                    benefitMysql.insertRow();
                    benefitMysql.moveToCurrentRow();
                }
            } catch (SQLException ex) {
                Logger.getLogger(EdboBenefits.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return json.toJson(submitStatus);
    }

}
