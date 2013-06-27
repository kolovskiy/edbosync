
package edbosync;

/**
 * Информация о статусе попытки передать информации
 *
 * @author С.В. Чопоров
 */
public class SubmitStatus {

    /**
     * Флаг наличия ошибки
     */
    private boolean error = true;
    /**
     * GUID из ЕДБО (в случае успеха)
     */
    private String guid = "";
    /**
     * ИДентификатор из ЕДБО (в случае успеха)
     */
    private int id;

    /**
     * Получить значение флага об ошибке
     *
     * @return значение флага об ошибке
     */
    public boolean isError() {
        return error;
    }

    /**
     * Установить значение флага об ошибке
     *
     * @param error новое значение флага об ошибке
     */
    public void setError(boolean error) {
        this.error = error;
    }

    /**
     * Получить GUID
     *
     * @return GUID
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Установить GUID
     *
     * @param guid новое значение GUID
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    /**
     * Получить идентификатор
     *
     * @return идентификатор
     */
    public int getId() {
        return id;
    }

    /**
     * Установить идентификатор
     *
     * @param id новое значение идентификатора
     */
    public void setId(int id) {
        this.id = id;
    }
}
