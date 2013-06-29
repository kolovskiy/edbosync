package edbosync;

/**
 * Льгота персоны
 *
 * @author С.В. Чопоров
 */
public class PersonBenefit {

    /**
     * Идентификатор льготы (ЕДБО + MySQL)
     */
    private int id_Benefit;
    /**
     * Идентификатор записи о льготе персоны в базе ЕДБО
     */
    private int id_PersonBenefit;

    /**
     * Получить значение идентификатора льготы персоны
     *
     * @return Значение иеднтификатора льготы персоны
     */
    public int getId_Benefit() {
        return id_Benefit;
    }

    /**
     * Установить значение идентификатора льготы персоны
     *
     * @param id_Benefit Новое значение иеднтификатора льготы персоны
     */
    public void setId_Benefit(int id_Benefit) {
        this.id_Benefit = id_Benefit;
    }

    /**
     * Получить значение идентификатора записи о льготе персоны в базе ЕДБО
     *
     * @return Значение идентификатора записи о льготе персоны в базе ЕДБО
     */
    public int getId_PersonBenefit() {
        return id_PersonBenefit;
    }

    /**
     * Установить значение идентификатора записи о льготе персоны в базе ЕДБО
     *
     * @param id_PersonBenefit Новое значение идентификатора записи о льготе
     * персоны в базе ЕДБО
     */
    public void setId_PersonBenefit(int id_PersonBenefit) {
        this.id_PersonBenefit = id_PersonBenefit;
    }
}
