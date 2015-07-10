/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edbosync;

import com.google.gson.Gson;
import java.util.List;
import ua.edboservice.ArrayOfDPersonEducationHistory2;
import ua.edboservice.ArrayOfDPersonEducations2;
import ua.edboservice.DPersonEducationHistory2;
import ua.edboservice.DPersonEducations2;
import ua.edboservice.EDBOPersonSoap;

/**
 *
 * @author matmod
 */
public class EdboEducationHistory {
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
    
    public String get2(String personCodeU) {
        Gson json = new Gson();
        ArrayOfDPersonEducationHistory2 aodpeh = soap.personEducationHistoryGet2(sessionGuid, actualDate, languageId, personCodeU, 0, 1);
        if (aodpeh == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonEducationHistory2> historyList = aodpeh.getDPersonEducationHistory2();
        return json.toJson(historyList);
    }
    
    public String educationsGet(String personCodeU) {
        Gson json = new Gson();
        ArrayOfDPersonEducations2 eduactionsArray = soap.personEducationsGet2(sessionGuid, actualDate, languageId, personCodeU, 0, 3, "");
        if (eduactionsArray == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonEducations2> eduactionsList = eduactionsArray.getDPersonEducations2();
        return json.toJson(eduactionsList);
    }
}
