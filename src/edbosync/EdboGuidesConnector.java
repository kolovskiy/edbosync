package edbosync;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDLastError;
import ua.edboservice.DLastError;
import ua.edboservice.EDBOGuides;
import ua.edboservice.EDBOGuidesSoap;

/**
 * Класс для соединения с сервером ЕДБО (справочники)
 *
 * @author Сергей Чопоров
 */
public class EdboGuidesConnector {

    protected EDBOGuides edboGuides = new EDBOGuides();
    /**
     * Экземпляр Soap-потока справочников
     */
    private EDBOGuidesSoap soap = null;
    /**
     * Идентификатор соап-сессии
     */
    protected String sessionGuid = "";
    /**
     * Текущая дата
     */
    protected String actualDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Calendar.getInstance().getTime());
    /**
     * Идентификатор языка (1 - укр)
     */
    protected int languageId = 1;
    /**
     * Идентификатор вступительной компании (2 - 2012, 3 - 2013, ...)
     */
    protected int seasonId = 6;
    /**
     * Ключ университета в ЕДБО
     */
    String universityKey = "ab1bc732-51f3-475c-bcfe-368363369020";

    /**
     * Конструктор по умолчанию создает соединение со службой EDBO Person
     */
    public EdboGuidesConnector() {
        login();
    }

    /**
     * Сборщик мусора закрывает соединение с ЕДБО при уничтожении экземпляра
     * класса
     */
    @Override
    protected void finalize() {
        try {
            logout();
        } finally {
            try {
                super.finalize();
            } catch (Throwable ex) {
                Logger.getLogger(EdboPersonConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Установка соединения со службой EDBO Guides
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected final boolean login() {
        // wsdl connection url:
        // http://edbo.znu.edu.ua:8080/EDBOGuides/EDBOGuides.asmx?WSDL
        // http://iskt-1.znu.edu.ua:9091/EDBOGuides/EDBOGuides.asmx?WSDL
        SoapConnectionData data = new SoapConnectionData();
        soap = edboGuides.getEDBOGuidesSoap();
        sessionGuid = soap.login(data.getSoapUser(), data.getSoapPassword(), 0, data.getApplicationKey());
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.err.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Закрытие соединения со службой EDBO Guides
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected final boolean logout() {
        String result = soap.logout(sessionGuid);
        if (result.length() > 0) {
            System.err.println(result);
            return false;
        }
        return true;
    }

    /**
     * Обработчик ошибок, которые возвращает сервер ЕДБО
     *
     * @return Строку с сообшением об ошибках
     */
    public String processErrors() {
        ArrayOfDLastError errorArray;
        errorArray = soap.getLastError(sessionGuid);
        String finalMessage = "";
        List<DLastError> errorList = errorArray.getDLastError();
        for (DLastError dError : errorList) {
            System.err.println(dError.getLastErrorDescription());
            finalMessage = finalMessage + dError.getLastErrorDescription() + " ";
        }
        return finalMessage;
    }
    
    /**
     * Обработчик ошибок, которые возвращает сервер ЕДБО
     *
     * @return Строка с сообщением об ошибках в формате json
     */
    public String processErrorsJson() {
        return "{\"error\":\"" + processErrors() + "\"}";
    }

    public EDBOGuidesSoap getSoap() {
        return soap;
    }

    public String getSessionGuid() {
        return sessionGuid;
    }

    public String getActualDate() {
        return actualDate;
    }

    public int getLanguageId() {
        return languageId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public String getUniversityKey() {
        return universityKey;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }

}
