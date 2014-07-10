package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDUniversityCourses;
import ua.edboservice.DUniversityCourses;
import ua.edboservice.EDBOGuidesSoap;

/**
 * Класс для работы с подготовительными курсами
 * @author Сергей Чопоров
 */
public class EdboCourses {
    /**
     * Получить справочник курсов из ЕДБО
     */
    public void loadDict() {
        EdboGuidesConnector edbo = new EdboGuidesConnector();
        EDBOGuidesSoap soap = edbo.getSoap();
        DataBaseConnector dbc = new DataBaseConnector();
        String sessionGuid = edbo.getSessionGuid();
        int idLanguage = edbo.getLanguageId();
        String actualDate = edbo.getActualDate();
        String universityCode = edbo.getUniversityKey();
        int idSeason = edbo.getSeasonId();
        ArrayOfDUniversityCourses arrayOfDUniversityCourses = soap.universityCoursesGet(sessionGuid, idLanguage, actualDate, universityCode, idSeason); //"ab1bc732-51f3-475c-bcfe-368363369020"
        if (arrayOfDUniversityCourses == null){
            System.err.println(edbo.processErrors());
            return;
        }
        List<DUniversityCourses> coursesList = arrayOfDUniversityCourses.getDUniversityCourses();
        for (DUniversityCourses duc : coursesList) {
            System.out.println(duc.getIdUniversityCourse() + "\t" + duc.getUniversityCourseName() + "\t" + duc.getUniversityCourseCode());
        }
    }
    
}
