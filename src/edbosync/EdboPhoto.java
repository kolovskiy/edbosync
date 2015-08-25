package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDPersonSOAPPhoto;
import ua.edboservice.DPersonSOAPPhoto;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для синхронизации фотографий
 * @author Сергей Чопоров
 */
public class EdboPhoto {
    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();
    public String load(String personCodeU){
        EDBOPersonSoap soap = edbo.getSoap();
        ArrayOfDPersonSOAPPhoto photoArray = soap.personSOAPPhotoGet(edbo.getSessionGuid(), edbo.getUniversityKey(), personCodeU);
        if (photoArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonSOAPPhoto> photoList = photoArray.getDPersonSOAPPhoto();
        String base64 = new String();
        for(DPersonSOAPPhoto dPhoto: photoList){
            if (dPhoto.getPersonPhotoIsActive() > 0)
                base64 = dPhoto.getPersonPhotoBase64String();
        }
        return base64;
    }
    public int add(String personCodeU, String base64) {
        EDBOPersonSoap soap = edbo.getSoap();
        return soap.personSOAPPhotoAdd(edbo.getSessionGuid(), edbo.getUniversityKey(), personCodeU, base64);
    }
}
