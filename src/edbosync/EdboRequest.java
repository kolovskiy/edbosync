package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonCourses;
import ua.edboservice.ArrayOfDPersonRequests2;
import ua.edboservice.DPersonCourses;
import ua.edboservice.DPersonRequests2;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для работы с заявками
 *
 * @author Сергей Чопоров
 */
public class EdboRequest {

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
    /**
     * Экземпляр соединения с БД
     */
    DataBaseConnector dbc = new DataBaseConnector();

    public String add(int personIdMySql, int personSpeciality) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);

        int edboIdPerson;
        String codeUPerson;
        int isResident;
        try {
            // идентификатор персоны в базе ЕДБО
            ResultSet person = dbc.executeQuery("SELECT `person`.`edboID`, `person`.`codeU`, `person`.`IsResident` FROM abiturient.person WHERE idPerson = " + personIdMySql + ";");
            person.next();
            edboIdPerson = person.getInt(1);
            codeUPerson = person.getString(2);
            isResident = person.getInt(3);
            if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Неможливо додати заяву для персони, що не пройшла синхронизацію з ЄДБО.");
                return json.toJson(submitStatus);
            }
            ResultSet personRequestOlympiadRS = dbc.executeQuery(""
                    + "SELECT `personspeciality`.`OlympiadID` "
                    + "FROM abiturient.personspeciality "
                    + "WHERE idPersonSpeciality = " + personSpeciality + ";");
            int idOlympiadAward = 0; // идентификатор олимпиады персоны
            int personOlympiadIdEdbo = 0; // идентификатор записи об олимпиаде персоны в ЕДБО
            String personRequestOlympiadAwardBonus = ""; // бонус за олимпиаду
            if (personRequestOlympiadRS.next()) {
                idOlympiadAward = personRequestOlympiadRS.getInt("OlympiadID");
            }
            if (idOlympiadAward != 0) {
                // в заявке есть олимпиада
                // дополнительный балл
                ResultSet olympBonus = dbc.executeQuery("SELECT OlympiadAwardBonus FROM abiturient.olympiadsawards WHERE OlympiadAwardID = " + idOlympiadAward + ";");
                if (olympBonus.next()) {
                    personRequestOlympiadAwardBonus = olympBonus.getString(1);
                }
                // синхронизация списка олимпиад с ЕДБО
                EdboOlympiads edboOlympiads = new EdboOlympiads();
                edboOlympiads.sync(codeUPerson, personIdMySql);

            }
            ResultSet request = dbc.executeQuery(""
                    + "SELECT * "
                    + "FROM abiturient.personspeciality "
                    + "WHERE idPersonSpeciality = " + personSpeciality + ";");
            if (request.next()) {
                int originalDocumentsAdd = (request.getInt("isCopyEntrantDoc") == 1) ? 0 : 1;
                int isNeedHostel = request.getInt("isNeedHostel");
                String codeOfBusiness = "";
                int qualificationId = request.getInt("QualificationID");
                String courseId = Integer.toString(request.getInt("CourseID"));
                codeOfBusiness = String.format("%05d", request.getInt("RequestNumber"));
                int idPersonEntranceType = request.getInt("EntranceTypeID");
                int idPersonExamenationCause = request.getInt("CausalityID");
                int idUniversityQuota1 = (request.getInt("Quota1") == 1) ? 1506 : 0;
                int idUniversityQuota2 = (request.getInt("Quota2") == 1) ? 1681 : 0;
                int isBudget = request.getInt("isBudget");
                int isContract = request.getInt("isContract");
                int idPersonEducationForm = request.getInt("EducationFormID");
                int idPersonCourse = request.getInt("CoursedpID");
                String personRequestCourseBonus = Float.toString(request.getFloat("CoursedpBall"));
                int isHigherEducation = request.getInt("isHigherEducation");
                int skipDocumentValue = request.getInt("SkipDocumentValue");
                int idDocumentSubject1 = request.getInt("DocumentSubject1");
                int idDocumentSubject2 = request.getInt("DocumentSubject2");
                int idDocumentSubject3 = request.getInt("DocumentSubject3");
                int specialityId = request.getInt("SepcialityID");
                String universitySpecialitiesCode = "";
                int idPersonDocument = request.getInt("EntrantDocumentID");
                int languageExId = request.getInt("LanguageExID");
                int edboId = request.getInt("edboID");
                // если запись уже была добавлена, то обновляем ее поля в ЕДБО
                if (edboId != 0) {
                    if (soap.personRequestEdit2(sessionGuid, // SessionGUID
                            request.getInt("edboID"), // Id_PersonRequest
                            originalDocumentsAdd, // OriginalDocumentsAdd
                            isNeedHostel, // IsNeedHostel
                            codeOfBusiness, // CodeOfBusiness
                            isBudget, // IsBudget
                            isContract, // IsContract
                            isHigherEducation, // IsHigherEducation
                            skipDocumentValue, // SkipDocumentValue
                            languageExId, // Id_LanguageEx
                            0, // Id_ForeignType
                            (isResident == 1) ? 0 : 1 // IsForeignWay
                            ) == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка редагування заявки  :  " + edbo.processErrors() + "<br />");
//                        return json.toJson(submitStatus);
                    } else {
                        submitStatus.setError(false);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Заявку успішно відредаговано.<br />");
//                        return json.toJson(submitStatus);
                    } // if - else
                } else {
                    // иначе добавляем заявку в ЕДБО
                    System.out.println("original  " + originalDocumentsAdd);
                    System.out.println("additional Ball     " + personRequestCourseBonus);

                    if (idDocumentSubject1 != 0) {
                        // есть первый предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject1 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject1 + ";");
                        if (subject1.next()) {
                            idDocumentSubject1 = subject1.getInt(1);
                        }
                    }
                    if (idDocumentSubject2 != 0) {
                        // есть второй предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject2 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject2 + ";");
                        if (subject2.next()) {
                            idDocumentSubject2 = subject2.getInt(1);
                        }
                    }
                    if (idDocumentSubject3 != 0) {
                        // есть третий предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject3 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM abiturient.documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject3 + ";");
                        if (subject3.next()) {
                            idDocumentSubject3 = subject3.getInt(1);
                        }
                    }
                    ResultSet specCodeRS = dbc.executeQuery(""
                            + "SELECT SpecialityKode "
                            + "FROM abiturient.specialities "
                            + "WHERE idSpeciality = " + specialityId + ";");
                    if (specCodeRS.next()) {
                        universitySpecialitiesCode = specCodeRS.getString(1);
                    }
                    ResultSet docCodeRs = dbc.executeQuery(""
                            + "SELECT edboID "
                            + "FROM abiturient.documents "
                            + "WHERE idDocuments = " + idPersonDocument + ";");
                    if (docCodeRs.next()) {
                        idPersonDocument = docCodeRs.getInt(1);
                    }
                    // синхронизация подготовительных курсов
                    if (idPersonCourse != 0) {
                        int personCourseIdold = idPersonCourse;
                        // 1 проверям наличие записий о курсах персоны в нашей базе
                        ArrayOfDPersonCourses coursesArray = soap.personCoursesGet(sessionGuid, actualDate, languageId, edboIdPerson, edbo.getSeasonId(), edbo.getUniversityKey());
                        List<DPersonCourses> coursesList = coursesArray.getDPersonCourses();
                        for (DPersonCourses courses : coursesList) {
                            ResultSet coursesIdRS = dbc.executeQuery("SELECT idCourseDP \n"
                                    + "FROM abiturient.coursedp\n"
                                    + "WHERE guid LIKE \"" + courses.getUniversityCourseCode() + "\";");
                            coursesIdRS.next();
                            int coursesIdLocal = coursesIdRS.getInt(1);
                            coursesIdRS.close();
                            ResultSet coursesRS = dbc.executeQuery(""
                                    + "SELECT * \n"
                                    + "FROM abiturient.personcoursesdp \n"
                                    + "WHERE \n"
                                    + "PersonID = " + personIdMySql + " \n"
                                    + "AND\n"
                                    + "guid LIKE \"" + courses.getUniversityCourseCode() + "\";");
                            if (!coursesRS.next()) {
                                // если нет то заносим
                                coursesRS.moveToInsertRow();
                                coursesRS.updateInt("PersonID", personIdMySql);
                                coursesRS.updateInt("CourseDPID", coursesIdLocal);
                                coursesRS.updateInt("edboID", courses.getIdPersonCourse());
                                coursesRS.updateString("guid", courses.getUniversityCourseCode());
                                coursesRS.insertRow();
                                coursesRS.moveToCurrentRow();
                            }
                        }
                        // 2 проверяем наличие строки соответствующими курсами у персоны
                        ResultSet coursesGuidRS = dbc.executeQuery("SELECT guid\n"
                                + "FROM `abiturient`.`coursedp` \n"
                                + "WHERE \n"
                                + "idCourseDP = " + personCourseIdold + ";");
                        coursesGuidRS.next();
                        String coursesGuid = coursesGuidRS.getString(1);
                        ResultSet coursesRS = dbc.executeQuery(""
                                + "SELECT * \n"
                                + "FROM abiturient.personcoursesdp \n"
                                + "WHERE \n"
                                + "PersonID = " + personIdMySql + " \n"
                                + "AND\n"
                                + "CourseDPID = " + personCourseIdold + ";");
                        if (coursesRS.next() && coursesRS.getInt("edboID") != 0) {
                            idPersonCourse = coursesRS.getInt("edboID");
                        } else {
                            // если не найдено, то нет и в едбо, значит обновляем
                            idPersonCourse = soap.personCoursesAdd(sessionGuid, languageId, edboIdPerson, coursesGuid, 0, edbo.getSeasonId(), "");
                            coursesRS.moveToInsertRow();
                            coursesRS.updateInt("PersonID", personIdMySql);
                            coursesRS.updateInt("CourseDPID", personCourseIdold);
                            coursesRS.updateInt("edboID", idPersonCourse);
                            coursesRS.insertRow();
                            coursesRS.moveToCurrentRow();
                        }
                        if (idPersonCourse == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            System.out.println(coursesGuid);
                            submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати курси  :  " + edbo.processErrors() + "<br />");
                            System.out.println(edbo.getSeasonId() + " " + codeUPerson + " " + universitySpecialitiesCode + " " + idPersonEducationForm + " " + idPersonDocument);
                            return json.toJson(submitStatus);
                        }
                    }
                    System.out.println(idPersonDocument);
//                    System.out.println(idPersonExamenationCause + "\t" + idPersonEntranceType+ "\tCause: " + ((idPersonExamenationCause != 0 && idPersonEntranceType != 1) ? idPersonExamenationCause : ((idPersonEntranceType == 1) ? 0 : 100)));
                    if (soap.personRequestCheckCanAdd(sessionGuid, edbo.getSeasonId(), codeUPerson, universitySpecialitiesCode, 0, idPersonEducationForm, idPersonDocument, 0, "") == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати заявку  :  " + edbo.processErrors() + "<br />");
                        System.out.println(edbo.getSeasonId() + " " + codeUPerson + " " + universitySpecialitiesCode + " " + idPersonEducationForm + " " + idPersonDocument);
                        return json.toJson(submitStatus);
                    } else {
                        System.out.println("Beeengooooooooooooooooooo");
                    }

                    edboId = soap.personRequestAdd2(sessionGuid, // SessionGUID,
                            edbo.getSeasonId(), // Id_PersonRequestSeasons,
                            codeUPerson, // PersonCodeU,
                            universitySpecialitiesCode, // UniversitySpecialitiesKode,
                            originalDocumentsAdd, // OriginalDocumentsAdd,
                            isNeedHostel, // IsNeedHostel,
                            codeOfBusiness, // CodeOfBusiness,
                            (idPersonEntranceType != 0) ? idPersonEntranceType : 2, // Id_PersonEnteranceTypes,
                            ((idPersonExamenationCause != 0 && idPersonEntranceType != 1) ? idPersonExamenationCause : ((idPersonEntranceType == 1) ? 0 : 100)), // Id_PersonRequestExaminationCause,
                            idUniversityQuota1, // Id_UniversitySpecialitiesQuota1,
                            idUniversityQuota2, // Id_UniversitySpecialitiesQuota2,
                            0, // Id_UniversitySpecialitiesQuota3,
                            0, // RequestFromEB,
                            isBudget, // IsBudget,
                            isContract, // IsContract,
                            idPersonEducationForm, // Id_PersonEducationForm,
                            idDocumentSubject1, // Id_PersonDocumentSubject1,
                            idDocumentSubject2, // Id_PersonDocumentSubject2,
                            idDocumentSubject3, // Id_PersonDocumentSubject3,
                            idPersonCourse, // Id_PersonCourse,
                            personRequestCourseBonus, // PersonRequestCourseBonus,
                            personOlympiadIdEdbo, //idOlympiadAward, // 22
                            personRequestOlympiadAwardBonus, // PersonRequestOlympiadAwardBonus,
                            idPersonDocument, // Id_PersonDocument,
                            0, // AutoSelectZnoSubjects,
                            "", // RequestEnteranceCodes,
                            isHigherEducation, // IsHigherEducation,
                            skipDocumentValue, // SkipDocumentValue,
                            0, // Id_PersonBenefit1,
                            0, // Id_PersonBenefit2,
                            0, // Id_PersonBenefit3,
                            languageExId, // Id_LanguageEx,
                            0, // Id_ForeignType
                            (isResident == 1) ? 0 : 1, // IsForeignWay
                            0 // RequestPriority
                    );
                }
                if (edboId == 0) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання заявки  :  " + edbo.processErrors() + "<br />");
                    return json.toJson(submitStatus);
                } else {
                    dbc.executeUpdate("UPDATE `abiturient`.`personspeciality`\n"
                            + "SET\n"
                            + "`edboID` = " + edboId + "\n"
                            + "WHERE idPersonSpeciality = " + personSpeciality + ";");
                    ResultSet benefitsRS = dbc.executeQuery("SELECT * FROM personspecialitybenefits join personbenefits on PersonBenefitID = idPersonBenefits where `PersonSpecialityID` = "
                            + personSpeciality + ";");
                    while (benefitsRS.next()) {
//                            int idBenefit = benefitsRS.getInt("BenefitID");
//                            if (idBenefit != 41 || (idBenefit == 41 && idPersonCourse != 0)) {
                        int result = soap.personRequestBenefitsAdd(sessionGuid, actualDate, languageId, edboId, benefitsRS.getInt("edboID"));
                        if (result == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання пільги до заявки  :  " + edbo.processErrors() + "<br />");
                        } else {
                            submitStatus.setMessage(submitStatus.getMessage() + "До заявки успішно додано пільгу<br />");
                        }
//                            }

                    }
                    submitStatus.setError(false);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "Заявку успішно додано до ЄДБО<br />");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
            return json.toJson(submitStatus);
        }
//            mySqlConnectionClose();
        return json.toJson(submitStatus);
    }

    public String load(String personCodeU, int personRequestId) {
        Gson json = new Gson();
        ArrayOfDPersonRequests2 aodpr = soap.personRequestsGet2(sessionGuid, actualDate, languageId, personCodeU, edbo.getSeasonId(), personRequestId, "", 1, 0, "");
        if (aodpr == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonRequests2> dprs = aodpr.getDPersonRequests2();
        for (DPersonRequests2 dpr : dprs) {
            System.out.println(dpr.getUniversitySpecialitiesKode());
        }
        return json.toJson(dprs);
    }
}
