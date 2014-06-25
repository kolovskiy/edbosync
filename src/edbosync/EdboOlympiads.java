package edbosync;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import ua.edboservice.ArrayOfDPersonOlympiadsAwards;
import ua.edboservice.DPersonOlympiadsAwards;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для обработки школьных олимпиад в ЕДБО
 *
 * @author Сергей Чопоров
 */
public class EdboOlympiads {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();

    /**
     * Получить идентификаторы олимпиад персоны из ЕДБО
     *
     * @param personCodeU Код U персоны
     * @return Массив идентификаторов формате json
     */
    public String load(String personCodeU) {
        EDBOPersonSoap soap = edbo.getSoap();
        Gson json = new Gson();
        ArrayList<Integer> olympiadId = new ArrayList<Integer>();
        ArrayOfDPersonOlympiadsAwards awardsArray = soap.personOlympiadsAwardsGet(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), personCodeU, edbo.getSeasonId());
        if (awardsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonOlympiadsAwards> awardsList = awardsArray.getDPersonOlympiadsAwards();
        for (DPersonOlympiadsAwards award : awardsList) {
            olympiadId.add(award.getIdOlympiadAward());
        }
        return json.toJson(olympiadId);
    }
}
