package edbosync;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import ua.edboservice.ArrayOfDPersonDocuments;
import ua.edboservice.ArrayOfDPersonDocumentsSubjects;
import ua.edboservice.DPersonDocuments;
import ua.edboservice.DPersonDocumentsSubjects;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для синхронизации документов с ЕДБО
 *
 * @author Сергей Чопоров
 */
public class EdboDocuments {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();

    /**
     * Получить контакты персоны из базы ЕДБО
     *
     * @param personCodeU Код U персоны
     * @return Массив контактов в формате json
     */
    public String load(String personCodeU) {
        EDBOPersonSoap soap = edbo.getSoap();
        Gson json = new Gson();
        ArrayOfDPersonDocuments documentsArray = soap.personDocumentsGet(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), personCodeU, 0, 0, "", -1);
        if (documentsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        ArrayList<PersonDocument> documents = new ArrayList<PersonDocument>();
        List<DPersonDocuments> documentsList = documentsArray.getDPersonDocuments();
        for (DPersonDocuments dDocument : documentsList) {
            PersonDocument document = new PersonDocument();
            document.setAttestatValue(dDocument.getAtestatValue());
            document.setDateGet(dDocument.getDocumentDateGet().toGregorianCalendar());
            document.setId_Document(dDocument.getIdPersonDocument());
            document.setId_Type(dDocument.getIdPersonDocumentType());
            document.setIssued(dDocument.getDocumentIssued());
            document.setNumber(dDocument.getDocumentNumbers());
            document.setSeries(dDocument.getDocumentSeries());
            document.setZnoPin(dDocument.getZNOPin());

            if (document.getId_Type() == 4) {
                // документ является сертификатом ЗНО
                ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>();
                ArrayOfDPersonDocumentsSubjects subjectsArray = soap.personDocumentsSubjectsGet(edbo.getSessionGuid(), edbo.getActualDate(), edbo.getLanguageId(), document.getId_Document(), dDocument.getIdPerson(), document.getId_Type());
                if (subjectsArray == null) {
                    // возникла ошибка при получении данных из ЕДБО
                    return edbo.processErrorsJson();
                }
                List<DPersonDocumentsSubjects> subjectsList = subjectsArray.getDPersonDocumentsSubjects();
                for (DPersonDocumentsSubjects dSubject : subjectsList) {
                    DocumentSubject subject = new DocumentSubject();
                    subject.setId_Subject(dSubject.getIdSubject());
                    subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                    subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                    subjects.add(subject);
                }
                document.setSubjects(subjects);
            }

            documents.add(document);
        }
        return json.toJson(documents);
    }
}
