package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDForeignTypes;
import ua.edboservice.DForeignTypes;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для работы со справочником ForeignTypes
 * @author Сергей Чопоров
 */
public class EdboForeignTypes {
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
    public void load(){
        ArrayOfDForeignTypes arrayOfDForeignTypes = soap.foreignTypesGet(sessionGuid);
        if (arrayOfDForeignTypes == null){
            System.err.println(edbo.processErrors());
            return;
        }
        List<DForeignTypes> dForeignTypeses = arrayOfDForeignTypes.getDForeignTypes();
        for (DForeignTypes dft : dForeignTypeses){
            System.out.println(dft.getIdForeignType() + "\t" + dft.getForeignTypeName());
        }
    }
}
