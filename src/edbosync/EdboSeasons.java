package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDPersonRequestSeasons;
import ua.edboservice.DPersonRequestSeasons;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для работы с сезонами вступительной компании
 *
 * @author Сергей Чопоров
 */
public class EdboSeasons {
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
     * Идентификатор языка
     */
    protected int idLanguage = edbo.getLanguageId();
    /**
     * Текущая дата
     */
    protected String actualDate = edbo.getActualDate();
    public void load(){
        ArrayOfDPersonRequestSeasons seasonsArray = soap.personRequestSeasonsGet(sessionGuid, idLanguage, actualDate, 0, 0, 0);
        if (seasonsArray == null){
            System.err.println(edbo.processErrors());
            return;
        }
        List<DPersonRequestSeasons> seasonsList = seasonsArray.getDPersonRequestSeasons();
        for (DPersonRequestSeasons season : seasonsList) {
            System.out.println(season.getIdPersonRequestSeasons() + "\t" + season.getName() + "\t" + season.getPersonEducationFormName());
        }
    }
}
