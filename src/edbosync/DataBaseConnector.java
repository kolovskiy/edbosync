package edbosync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс-соединение с базой данных
 *
 * @author Сергей Чопоров
 * @version 1.0.0
 */
public class DataBaseConnector {

    /**
     * Обработчик соединения с базой данных
     */
    protected Connection connection = null;
    /**
     * Обработчик запросов к базе данных
     */
    protected Statement statement = null;

    /**
     * Конструктор по умолчанию
     */
    public DataBaseConnector() {
        connectMySql();
    }

    /**
     * Сборщик мусора закрывает соединение с БД при уничтожении экземпляра
     * класса
     */
    @Override
    protected void finalize() {
        try {
            closeConnection();
        } finally {
            try {
                super.finalize();
            } catch (Throwable ex) {
                Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Метод устанавливает соединение з БД MySQL
     *
     * @return true, если соединение успешно установлено, false - иначе
     */
    protected final boolean connectMySql() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // загрузка драйвера базы данных
            // информация о соединении
            MySqlConnectionData data = new MySqlConnectionData();
            // создание соединения с БД
            try {
                connection = DriverManager.getConnection(data.getMySqlConnectionUrl(), data.getMySqlUser(), data.getMySqlPassword());
            } catch (SQLException ex) {
                Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            // создание обработчика запросов
            try {
                statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            } catch (SQLException ex) {
                Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * Метод закрывает соединение з БД
     *
     * @return true, если соединение успешно закрыто, false - иначе
     */
    protected final boolean closeConnection() {
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * Выполнить SQL-запрос
     *
     * @param query Текст запроса
     * @return Результат выполнения запроса
     */
    public ResultSet executeQuery(String query) {
        try {
            return statement.executeQuery(query);
        } catch (SQLException ex) {
            Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Выполнить SQL-запрос на обновление
     *
     * @param query Текст запроса
     * @return Количество обработанных строк
     */
    public int executeUpdate(String query) {
        try {
            return statement.executeUpdate(query);
        } catch (SQLException ex) {
            Logger.getLogger(DataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    /**
     * Получить экземпляр объекта соединения с БД
     *
     * @return Экземпляр объекта соединения с БД
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Получить экземпляр объекта обработчика запросов к БД
     *
     * @return Экземпляр объекта обработчика запросов к БД
     */
    public Statement getStatement() {
        return statement;
    }

}
