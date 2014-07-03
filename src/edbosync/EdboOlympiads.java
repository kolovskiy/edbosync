package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    /**
     * Синхронизировать списки олимпиад БД и ЕДБО
     *
     * @param personCodeU Код U персоны
     * @param personId Идентификатор персоны в базе ЕДБО
     */
    public void sync(String personCodeU, int personId) {
        DataBaseConnector dbc = new DataBaseConnector(); // Соединение с БД
        EDBOPersonSoap soap = edbo.getSoap();
        
        // ЕДБО ----> БД
        ArrayOfDPersonOlympiadsAwards awardsArray = soap.personOlympiadsAwardsGet(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), personCodeU, edbo.getSeasonId());
        if (awardsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            System.err.println(edbo.processErrorsJson());
        } else {
            List<DPersonOlympiadsAwards> awardsList = awardsArray.getDPersonOlympiadsAwards();
            for (DPersonOlympiadsAwards award : awardsList) {
                String sql = "SELECT * "
                        + "FROM personolympiad "
                        + "WHERE PersonID = " + personId + " AND OlympiadAwarID = " + award.getIdOlympiadAward() + ";";
                try {
                    ResultSet personOlympiadsRS = dbc.executeQuery(sql);
                    if (personOlympiadsRS.next()) {
                        // олимпиада уже была добавлена
                        personOlympiadsRS.updateInt("edboID", award.getIdPersonOlympiadAward());
                        personOlympiadsRS.updateRow();
                        System.out.println("Обновлена олимпиада: " + award.getIdPersonOlympiadAward());
                    } else {
                        personOlympiadsRS.moveToInsertRow();
                        personOlympiadsRS.updateInt("PersonID", personId);
                        personOlympiadsRS.updateInt("OlympiadAwarID", award.getIdOlympiadAward());
                        personOlympiadsRS.updateInt("edboID", award.getIdPersonOlympiadAward());
                        personOlympiadsRS.insertRow();
                        personOlympiadsRS.moveToCurrentRow();
                        System.out.println("Вставлена олимпиада: " + award.getIdPersonOlympiadAward());
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        // БД ----> ЕДБО
        String sql = "SELECT * FROM personolympiad WHERE PersonID = " + personId + ";";
        try {
            ResultSet olympiad = dbc.executeQuery(sql);
            while (olympiad.next()) {
                if (olympiad.getInt("edboID") == 0) {
                    int edboID = soap.personOlympiadsAwardsAdd(edbo.getSessionGuid(), edbo.getLanguageId(), personId, olympiad.getInt("OlympiadAwarID"));
                    olympiad.updateInt("edboID", edboID);
                    olympiad.updateRow();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
