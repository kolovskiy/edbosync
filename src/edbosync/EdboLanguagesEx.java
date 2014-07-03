package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDLanguagesEx;
import ua.edboservice.DLanguagesEx;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для роботы со справочником LanguagesEx
 *
 * @author Сергей Чопоров
 */
public class EdboLanguagesEx {

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
     * Экземпляр соединения с БД
     */
    DataBaseConnector dbc = new DataBaseConnector();

    /**
     * Получить данные из справочника ЕДБО
     */
    public void load() {
        ArrayOfDLanguagesEx languagesExArray = soap.languagesExGet(sessionGuid);
        if (languagesExArray == null) {
            System.err.println(edbo.processErrors());
            return;
        }
        List<DLanguagesEx> languagesExList = languagesExArray.getDLanguagesEx();
        for (DLanguagesEx dLanguagesEx : languagesExList) {
            System.out.println(dLanguagesEx.getIdLanguageEx() + "\t" + dLanguagesEx.getLanguageExName());
            String query = "INSERT INTO `languagesex`\n"
                    + "(`IdLanguageEx`,\n"
                    + "`LanguageExName`)\n"
                    + "VALUES\n"
                    + "("+ dLanguagesEx.getIdLanguageEx() +",\n"
                    + "'" + dLanguagesEx.getLanguageExName().replace("'", "\\'") + "');";
//            System.out.println(query);
            dbc.executeUpdate(query);
        }
    }
}
