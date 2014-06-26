package edbosync;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
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
     * Получить сведения о льготах персоны из ЕДБО
     *
     * @param personId Идентификатор персоны в ЕДБО
     * @return
     */
    public String load(int personId) {
        ArrayList<PersonBenefit> personBenefits = new ArrayList<PersonBenefit>();
        EDBOPersonSoap soap = edbo.getSoap();
        Gson json = new Gson();
        ArrayOfDPersonBenefits2 benefitsArray = soap.personBenefitsGet2(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), personId);
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
}
