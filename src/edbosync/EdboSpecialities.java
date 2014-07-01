package edbosync;

import java.util.List;
import ua.edboservice.ArrayOfDUniversityFacultetSpecialities;
import ua.edboservice.ArrayOfDUniversityFacultets;
import ua.edboservice.DUniversityFacultetSpecialities;
import ua.edboservice.DUniversityFacultets;
import ua.edboservice.EDBOGuidesSoap;

/**
 * Класс для работы со списком специальностей в ЕДБО
 *
 * @author Сергей Чопоров
 */
public class EdboSpecialities {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboGuidesConnector edbo = new EdboGuidesConnector();
    /**
     * Экземпляр потока SOAP
     */
    protected EDBOGuidesSoap soap = edbo.getSoap();
    /**
     * Экземпляр соединения с базой данных
     */
    DataBaseConnector dbc = new DataBaseConnector();

    /**
     * Синхронизировать список специальностей
     */
    public void sync() {
        ArrayOfDUniversityFacultets facultetsArray = soap.universityFacultetsGet(edbo.getSessionGuid(), edbo.getUniversityKey(), "", edbo.getLanguageId(), edbo.getActualDate(), 1, "20", 1, -1, 0, -1);
        if (facultetsArray == null) {
            System.err.println(edbo.processErrors());
            return;
        }
        List<DUniversityFacultets> facultetsList = facultetsArray.getDUniversityFacultets();
        for (DUniversityFacultets facultet : facultetsList) {
            System.out.println(facultet.getUniversityFacultetFullName() + "(" + facultet.getUniversityFacultetShortName() + "):");
            ArrayOfDUniversityFacultetSpecialities specialitiesArray = soap.universityFacultetSpecialitiesGet(edbo.getSessionGuid(), edbo.getUniversityKey(), facultet.getUniversityFacultetKode(), "", edbo.getLanguageId(), edbo.getActualDate(), edbo.getSeasonId(), 3, "", "", "", "");
            if (specialitiesArray == null) {
                System.err.println(edbo.processErrors());
                continue;
            }
            List<DUniversityFacultetSpecialities> specialitiesList = specialitiesArray.getDUniversityFacultetSpecialities();
            for (DUniversityFacultetSpecialities speciality : specialitiesList) {
                String mon_kod = "";
                if (speciality.getSpecClasifierCode() != null && !speciality.getSpecClasifierCode().isEmpty()) {
                    mon_kod = speciality.getSpecClasifierCode();
                } else {
                    mon_kod = speciality.getSpecSpecialityClasifierCode();
                }
                System.out.println(speciality.getIdUniversitySpecialities() + ","
                        + "'" + speciality.getSpecSpecialityName() + "',"
                        + "'" + speciality.getSpecDirectionName() + "',"
                        + "'" + speciality.getSpecSpecializationName() + "',"
                        + "'" + speciality.getUniversitySpecialitiesKode() + "',"
                        + facultet.getIdUniversityFacultet() + ","
                        + mon_kod + ","
                        + speciality.getUniversitySpecialitiesContractCount() + ","
                        + speciality.getIdPersonEducationForm() + ", begin " + speciality.getDateBeginPersonRequestSeason().toString()
                        + " end " + speciality.getDateEndPersonRequestSeason().toString());
                System.out.println("");
            }
        }
    }
}
