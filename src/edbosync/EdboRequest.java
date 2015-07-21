package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonCourses;
import ua.edboservice.ArrayOfDPersonRequestDocumentSubjects;
import ua.edboservice.ArrayOfDPersonRequestStatusTypes;
import ua.edboservice.ArrayOfDPersonRequests2;
import ua.edboservice.ArrayOfDPersonRequestsAllPriority;
import ua.edboservice.ArrayOfDPersonRequestsStatuses2;
import ua.edboservice.DPersonCourses;
import ua.edboservice.DPersonRequestDocumentSubjects;
import ua.edboservice.DPersonRequestStatusTypes;
import ua.edboservice.DPersonRequests2;
import ua.edboservice.DPersonRequestsAllPriority;
import ua.edboservice.DPersonRequestsStatuses2;
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
            ResultSet person = dbc.executeQuery("SELECT `person`.`edboID`, `person`.`codeU`, `person`.`IsResident` FROM person WHERE idPerson = " + personIdMySql + ";");
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
                    + "FROM personspeciality "
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
                ResultSet olympBonus = dbc.executeQuery("SELECT OlympiadAwardBonus FROM olympiadsawards WHERE OlympiadAwardID = " + idOlympiadAward + ";");
                if (olympBonus.next()) {
                    personRequestOlympiadAwardBonus = olympBonus.getString(1);
                }
                // синхронизация списка олимпиад с ЕДБО
                EdboOlympiads edboOlympiads = new EdboOlympiads();
                edboOlympiads.sync(codeUPerson, personIdMySql);
                ResultSet personOlympiadsRS = dbc.executeQuery(""
                        + "SELECT * "
                        + "FROM personolympiad "
                        + "WHERE PersonID = " + personIdMySql + " AND OlympiadAwarID = " + idOlympiadAward + ";");
                if (personOlympiadsRS.first()) {
                    personOlympiadIdEdbo = personOlympiadsRS.getInt("edboID");

                } else {
                    // данная олимпиада вводится впервые ее нужно ставаить в БД
                    personOlympiadIdEdbo = soap.personOlympiadsAwardsAdd(sessionGuid, languageId, edboIdPerson, idOlympiadAward);
                }

                if (personOlympiadIdEdbo == 0) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання олімпіади до ЄДЕБО :  " + edbo.processErrors() + "<br />");
                    return json.toJson(submitStatus);
                } else {
                    submitStatus.setMessage(submitStatus.getMessage() + "До заявки додано олімпіаду, додатковий бал:" + personRequestOlympiadAwardBonus + ".<br />");
                }

            }
            ResultSet request = dbc.executeQuery(""
                    + "SELECT * "
                    + "FROM personspeciality "
                    + "WHERE idPersonSpeciality = " + personSpeciality + ";");
            if (request.next()) {
                int originalDocumentsAdd = (request.getInt("isCopyEntrantDoc") == 1) ? 0 : 1;
                int isNeedHostel = request.getInt("isNeedHostel");
                String codeOfBusiness = String.format("%05d", request.getInt("RequestNumber"));
//                int qualificationId = request.getInt("QualificationID");
//                String courseId = Integer.toString(request.getInt("CourseID"));
//                codeOfBusiness = String.format("%05d", request.getInt("RequestNumber"));
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
                int priority = request.getInt("priority");
                
                // синхронизация подготовительных курсов
                if (idPersonCourse != 0) {
                    int personCourseIdold = idPersonCourse;
                    // 1 проверям наличие записий о курсах персоны в нашей базе
                    ArrayOfDPersonCourses coursesArray = soap.personCoursesGet(sessionGuid, actualDate, languageId, edboIdPerson, edbo.getSeasonId(), edbo.getUniversityKey());
                    List<DPersonCourses> coursesList = coursesArray.getDPersonCourses();
                    for (DPersonCourses courses : coursesList) {
                        ResultSet coursesIdRS = dbc.executeQuery("SELECT idCourseDP \n"
                                + "FROM coursedp\n"
                                + "WHERE guid LIKE \"" + courses.getUniversityCourseCode() + "\";");
                        System.out.println(courses.getUniversityCourseCode());
                        if (coursesIdRS.next()) {
                            int coursesIdLocal = coursesIdRS.getInt(1);
                            coursesIdRS.close();
                            ResultSet coursesRS = dbc.executeQuery(""
                                    + "SELECT * \n"
                                    + "FROM personcoursesdp \n"
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
                    }
                    // 2 проверяем наличие строки соответствующими курсами у персоны
                    ResultSet coursesGuidRS = dbc.executeQuery("SELECT guid\n"
                            + "FROM `coursedp` \n"
                            + "WHERE \n"
                            + "idCourseDP = " + personCourseIdold + ";");
                    coursesGuidRS.next();
                    String coursesGuid = coursesGuidRS.getString(1);
                    ResultSet coursesRS = dbc.executeQuery(""
                            + "SELECT * \n"
                            + "FROM personcoursesdp \n"
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
                        submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати курси  :  " + edbo.processErrors() + "<br />\n");
                        System.out.println(edbo.getSeasonId() + " " + codeUPerson + " " + universitySpecialitiesCode + " " + idPersonEducationForm + " " + idPersonDocument);
                        return json.toJson(submitStatus);
                    }
                }
                
                // если запись уже была добавлена, то обновляем ее поля в ЕДБО
                if (edboId != 0) {
                    if (soap.personRequestEdit2(sessionGuid, // SessionGUID
                            edboId, // Id_PersonRequest
                            originalDocumentsAdd, // OriginalDocumentsAdd
                            isNeedHostel, // IsNeedHostel
                            codeOfBusiness, // CodeOfBusiness
                            isBudget, // IsBudget
                            isContract, // IsContract
                            isHigherEducation, // IsHigherEducation
                            skipDocumentValue, // SkipDocumentValue
                            languageExId, // Id_LanguageEx
                            0, // Id_ForeignType
                            0 // (isResident == 1) ? 0 : 1 // IsForeignWay
                    ) == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка редагування заявки  :  " + edbo.processErrors() + "<br />\n");
//                        return json.toJson(submitStatus);
                    } else {
                        submitStatus.setError(false);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Заявку успішно відредаговано.<br />\n");
//                        return json.toJson(submitStatus);
                    } // if - else
                    
                    if (idPersonCourse != 0 && soap.personRequestCoursesAdd(sessionGuid, languageId, edboId, idPersonCourse, personRequestCourseBonus) == 0){
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання курсів до заявки  :  " + edbo.processErrors() + "<br />\n");
                    } else {
                        submitStatus.setError(false);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Курси додано до заявки<br />\n");
                    }
                    
                    if (idOlympiadAward != 0 && soap.personRequestOlympiadsAwardsAdd(sessionGuid, languageId, edboId, idOlympiadAward, personRequestOlympiadAwardBonus) == 0)
                    {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання олімпіади до заявки  :  " + edbo.processErrors() + "<br />\n");
                    } else {
                        submitStatus.setError(false);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Олімпіаду додано до заявки<br />\n");
                    }
                } else {
                    // иначе добавляем заявку в ЕДБО
                    System.out.println("original  " + originalDocumentsAdd);
                    System.out.println("additional Ball     " + personRequestCourseBonus);

                    if (idDocumentSubject1 != 0) {
                        // есть первый предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject1 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject1 + ";");
                        if (subject1.next()) {
                            idDocumentSubject1 = subject1.getInt(1);
                        }
                    }
                    if (idDocumentSubject2 != 0) {
                        // есть второй предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject2 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject2 + ";");
                        if (subject2.next()) {
                            idDocumentSubject2 = subject2.getInt(1);
                        }
                    }
                    if (idDocumentSubject3 != 0) {
                        // есть третий предмет сертификата: выбираем его идентификатор из таблицы предметов
                        ResultSet subject3 = dbc.executeQuery(""
                                + "SELECT edboID "
                                + "FROM documentsubject "
                                + "WHERE idDocumentSubject = " + idDocumentSubject3 + ";");
                        if (subject3.next()) {
                            idDocumentSubject3 = subject3.getInt(1);
                        }
                    }
                    ResultSet specCodeRS = dbc.executeQuery(""
                            + "SELECT SpecialityKode "
                            + "FROM specialities "
                            + "WHERE idSpeciality = " + specialityId + ";");
                    if (specCodeRS.next()) {
                        universitySpecialitiesCode = specCodeRS.getString(1);
                    }
                    ResultSet docCodeRs = dbc.executeQuery(""
                            + "SELECT edboID "
                            + "FROM documents "
                            + "WHERE idDocuments = " + idPersonDocument + ";");
                    if (docCodeRs.next()) {
                        idPersonDocument = docCodeRs.getInt(1);
                    }
                    
                    System.out.println(idPersonDocument);
//                    System.out.println(idPersonExamenationCause + "\t" + idPersonEntranceType+ "\tCause: " + ((idPersonExamenationCause != 0 && idPersonEntranceType != 1) ? idPersonExamenationCause : ((idPersonEntranceType == 1) ? 0 : 100)));
                    if (soap.personRequestCheckCanAdd(sessionGuid, edbo.getSeasonId(), codeUPerson, universitySpecialitiesCode, 0, idPersonEducationForm, idPersonDocument, 0, "") == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + "Неможливо додати заявку  :  " + edbo.processErrors() + "<br />\n");
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
                            0, //(isResident == 1) ? 0 : 1, // IsForeignWay
                            priority // RequestPriority
                    );
                }
                if (edboId == 0) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання заявки  :  " + edbo.processErrors() + "<br />\n");
                    return json.toJson(submitStatus);
                } else {
                    dbc.executeUpdate("UPDATE `personspeciality`\n"
                            + "SET\n"
                            + "`edboID` = " + edboId + "\n"
                            + "WHERE idPersonSpeciality = " + personSpeciality + ";");
                    // льготы
                    EdboBenefits benefits = new EdboBenefits();
                    benefits.sync(personIdMySql); // принудительная синхронизация
                    //закидывание в заявку
                    ResultSet benefitsRS = dbc.executeQuery("SELECT * FROM personspecialitybenefits join personbenefits on PersonBenefitID = idPersonBenefits where `PersonSpecialityID` = "
                            + personSpeciality + ";");
                    while (benefitsRS.next()) {
//                            int idBenefit = benefitsRS.getInt("BenefitID");
//                            if (idBenefit != 41 || (idBenefit == 41 && idPersonCourse != 0)) {
                        int result = soap.personRequestBenefitsAdd(sessionGuid, actualDate, languageId, edboId, benefitsRS.getInt("edboID"));
                        if (result == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + "Помилка додавання пільги до заявки  :  " + edbo.processErrors() + "<br />\n");
                        } else {
                            submitStatus.setMessage(submitStatus.getMessage() + "До заявки успішно додано пільгу<br />\n");
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

    /**
     * Получить заявки персоны из ЕДБО
     *
     * @param personCodeU Код персоны в ЕДБО
     * @param personRequestId Идентификатор заяки (если указать ноль, то
     * выбираются все заявки)
     * @return Список заявок персоны в формате json
     */
    public String load(String personCodeU, int personRequestId) {
        Gson json = new Gson();
        ArrayOfDPersonRequests2 aodpr = soap.personRequestsGet2(sessionGuid, actualDate, languageId, personCodeU, edbo.getSeasonId(), personRequestId, "", 0, 0, "");
        if (aodpr == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonRequests2> dprs = aodpr.getDPersonRequests2();
        for (DPersonRequests2 dpr : dprs) {
            System.out.println(dpr.getUniversitySpecialitiesKode());
        }
        return json.toJson(dprs);
    }

    /**
     * Получить список идентификаторов заявок с заданными статусом,
     * квалификацией и датой создания
     *
     * @param idStatus Идентификатор статуса заявки
     * @param idQualification Идентификатор квлафикации заявки
     * @param createDate Дата создания заявки
     * @return Список идентификаторов в формате json
     */
    public String getIds(int idStatus, int idQualification, String createDate) {
        ArrayList<Integer> idPersonRequest = new ArrayList<Integer>();
        Gson json = new Gson();
        String sql = "SELECT * FROM personspeciality where DATE(CreateDate) = \""
                + createDate + "\" and StatusID = "
                + idStatus + " and QualificationID = " + idQualification + " and edboID is not null;";
        ResultSet resultSet = dbc.executeQuery(sql);
        try {
            while (resultSet.next()) {
                idPersonRequest.add(resultSet.getInt("edboID"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json.toJson(idPersonRequest);
    }

    /**
     * Изменить статус заявки
     *
     * @param idPersonRequest Идентификатор заявки в ЕДБО
     * @param idStatus Идентификатор статуса (новый)
     * @param numberProtocol Номер протокола решения приемной комисси
     * @param dateProtocol Дата протокола решения приемной комисси
     * @return Стаус попытки в формате json
     */
    public String changeStatus(int idPersonRequest, int idStatus, String numberProtocol, String dateProtocol) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        ArrayOfDPersonRequestsStatuses2 arrayOfDPersonRequestsStatuses2 = soap.personRequestsStatusesGet2(sessionGuid, languageId, idPersonRequest);
        if (arrayOfDPersonRequestsStatuses2 == null) {
            submitStatus.setError(true);
            submitStatus.setMessage(edbo.processErrors());
            return json.toJson(submitStatus);
        }
        List<DPersonRequestsStatuses2> dPersonRequestsStatuses2s = arrayOfDPersonRequestsStatuses2.getDPersonRequestsStatuses2();
        DPersonRequestsStatuses2 lastStatus = dPersonRequestsStatuses2s.get(0);
        int idUniversityEntrantWave = lastStatus.getIdUniversityEntrantWave();
        int submitResult = soap.personRequestsStatusChange2(sessionGuid, // SessionGUID
                idPersonRequest, // Id_PersonRequest
                idStatus, // Id_PersonRequestStatusType
                "", // Descryption
                idUniversityEntrantWave, // Id_UniversityEntrantWave
                -1, // IsBudejt
                -1, // IsContract
                numberProtocol, // NumberProtocol
                dateProtocol // DateProtocol
        );
        if (submitResult == 0) {
            submitStatus.setError(true);
            submitStatus.setMessage(edbo.processErrors());
            return json.toJson(submitStatus);
        } else {
            String query = "UPDATE `personspeciality`\n"
                    + "SET\n"
                    + "`StatusID` = " + idStatus + ",\n"
                    + "`NumberProtocol` = '" + numberProtocol + "',\n"
                    + "`DateProtocol` = '" + dateProtocol + "'\n"
                    + "WHERE `edboID` = " + idPersonRequest + ";";
            dbc.executeUpdate(query);
            String insQuery = "INSERT INTO `requeststatuseshistory`\n"
                    + "("
                    + "`PersonSpecialityID`,\n"
                    + "`PersonRequestStatusTypeID`,\n"
                    + "`NumberProtocol`,\n"
                    + "`DateProtocol`,\n"
                    + "`DateLastChange`)\n"
                    + "SELECT "
                    + "`idPersonSpeciality`, "
                    + "`StatusID`, "
                    + "`personspeciality`.`NumberProtocol`, "
                    + "STR_TO_DATE(`personspeciality`.`DateProtocol`, '%d.%m.%Y'), "
                    + "`personspeciality`.`Modified`\n"
                    + "FROM `personspeciality` "
                    + "WHERE edboID = " + idPersonRequest + ";";
            dbc.executeUpdate(insQuery);
            submitStatus.setMessage("Статус заявки змінено");
        }
        return json.toJson(submitStatus);
    }

    /**
     * Изменить статус заявок
     *
     * @param from Исходный статус
     * @param to Целевой статус
     * @param dateRequests Дата создания заявок
     * @param numberProtocol Номер протокола обработки заявок комиссией
     * @param dateProtocol Дата проотокола обработки заявок комиссией
     * @return Статус попытки в формате json
     */
    public String changeStatuses(int from, int to, String dateRequests, String numberProtocol, String dateProtocol) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        String sql = "SELECT * FROM personspeciality where DATE(CreateDate) = \""
                + dateRequests + "\" and StatusID = "
                + from + " and edboID is not null;";
        ResultSet resultSet = dbc.executeQuery(sql);
        try {
            while (resultSet.next()) {
                int idPersonRequest = resultSet.getInt("edboID");
                ArrayOfDPersonRequestsStatuses2 arrayOfDPersonRequestsStatuses2 = soap.personRequestsStatusesGet2(sessionGuid, languageId, idPersonRequest);
                if (arrayOfDPersonRequestsStatuses2 == null) {
                    System.err.println(idPersonRequest + " : " + edbo.processErrors());
                    break;
                }
                List<DPersonRequestsStatuses2> dPersonRequestsStatuses2s = arrayOfDPersonRequestsStatuses2.getDPersonRequestsStatuses2();
                DPersonRequestsStatuses2 lastStatus = dPersonRequestsStatuses2s.get(0);
                if (lastStatus.getIdPersonRequestStatusType() == from) {
                    int idUniversityEntrantWave = lastStatus.getIdUniversityEntrantWave();
                    int submitResult = soap.personRequestsStatusChange2(sessionGuid, // SessionGUID
                            idPersonRequest, // Id_PersonRequest
                            to, // Id_PersonRequestStatusType
                            "", // Descryption
                            idUniversityEntrantWave, // Id_UniversityEntrantWave
                            -1, // IsBudejt
                            -1, // IsContract
                            numberProtocol, // NumberProtocol
                            dateProtocol // DateProtocol
                    );
                    if (submitResult == 0) {
                        System.err.println(idPersonRequest + ": " + edbo.processErrors());
//                    break;
                    } else {
                        resultSet.updateInt("StatusID", to);
                        resultSet.updateString("NumberProtocol", numberProtocol);
                        resultSet.updateString("DateProtocol", dateProtocol);
                        resultSet.updateRow();
                        System.out.println(idPersonRequest + "\t:\tстатус заявки змінено.");
                    }
                } else {
                    System.err.println(idPersonRequest + "\tСтатус заявки не відповідає введеному: " + lastStatus.getIdPersonRequestStatusType() + "!=" + from);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json.toJson(submitStatus);
    }

    /**
     * Получение данных о предметах сертификатов ЗНО для заявки из ЕДБО
     *
     * @param idPersonRequest Идентификатор заявки (ЕДБО) персоны для которой
     * выбираются предметы
     * @return В случае успешного выполнения список прдметов в формате json
     */
    public String loadSubjects(int idPersonRequest) {
        Gson json = new Gson();
        ArrayOfDPersonRequestDocumentSubjects aodprds = soap.personRequestDocumentSubjectsGet(sessionGuid, actualDate, languageId, idPersonRequest);
        if (aodprds == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonRequestDocumentSubjects> dprds = aodprds.getDPersonRequestDocumentSubjects();
        return json.toJson(dprds);
    }

    public void loadRequestStatusTypes() {
        ArrayOfDPersonRequestStatusTypes aodprst = soap.personRequestStatusTypesGet(sessionGuid, actualDate, languageId);
        if (aodprst == null) {
            System.err.println(edbo.processErrors());
            return;
        }
        List<DPersonRequestStatusTypes> statusTypeses = aodprst.getDPersonRequestStatusTypes();
        for (DPersonRequestStatusTypes dprst : statusTypeses) {
            System.out.println(dprst.getIdPersonRequestStatusType() + " " + dprst.getPersonRequestStatusCode() + " " + dprst.getPersonRequestStatusTypeName() + " " + dprst.getPersonRequestStatusTypeDescription());
        }
    }

    /**
     * Добавить к заявкам с курсами льготы (чистка хвостов)
     */
    public void addCoursesBenefit() {
        String query = "SELECT \n"
                + "    *\n"
                + "FROM\n"
                + "    personspeciality\n"
                + "where\n"
                + "    CoursedpID > 0\n"
                + "        and edboID is not null;";
        ResultSet resultSet = dbc.executeQuery(query);
        class RequestWithCourses {

            public int edboID; //!< Идентификатор ЕДБО заявки
            public int idPersonSpeciality; //!< Идентификатор записи о специальности
            public int idPerson; //!< Идентификатор персоны
        }
        ArrayList<RequestWithCourses> rwcs = new ArrayList<RequestWithCourses>();
        try {
            while (resultSet.next()) {
                RequestWithCourses courses = new RequestWithCourses();
                courses.edboID = resultSet.getInt("edboID");
                courses.idPerson = resultSet.getInt("PersonID");
                courses.idPersonSpeciality = resultSet.getInt("idPersonSpeciality");
                rwcs.add(courses);
            }
            for (RequestWithCourses courses : rwcs) {
                EdboBenefits benefits = new EdboBenefits();
                benefits.sync(courses.idPerson);
                ResultSet benefitsRS = dbc.executeQuery("SELECT * FROM personspecialitybenefits join personbenefits on PersonBenefitID = idPersonBenefits where `PersonSpecialityID` = "
                        + courses.idPersonSpeciality + ";");
                EdboPersonConnector edboLocal = new EdboPersonConnector();
                EDBOPersonSoap soapLocal = edboLocal.getSoap();
                while (benefitsRS.next()) {
                    int result = soapLocal.personRequestBenefitsAdd(edboLocal.getSessionGuid(), actualDate, languageId, courses.edboID, benefitsRS.getInt("edboID"));
                    if (result == 0) {
                        System.err.println(courses.idPersonSpeciality + ": " + edboLocal.processErrors());
                    } else {
                        System.out.println(courses.idPersonSpeciality + ": " + "Добавлено льготу: " + benefitsRS.getInt("BenefitID"));
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String allPriorityGet(String personCodeU) {
        Gson json = new Gson();
        ArrayOfDPersonRequestsAllPriority aodprapp = soap.personRequestsAllPriorityGet(sessionGuid, personCodeU, edbo.getUniversityKey(), edbo.getSeasonId(), "");
        if (aodprapp == null) {
            return edbo.processErrorsJson();
        }
        List<DPersonRequestsAllPriority> allPriority = aodprapp.getDPersonRequestsAllPriority();
        return json.toJson(allPriority);
    }
    
    public String newPriorityGet(String personCodeU) {
        Gson json = new Gson();
        int priority = soap.personRequestsNewPriorityGet(sessionGuid, personCodeU, edbo.getUniversityKey(), edbo.getSeasonId());
        return json.toJson(priority);
    }
}
