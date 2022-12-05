package cdp.jabaorm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionUtil {
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find MySQL Driver", e);
        }
    }

    public static Connection getConnection() {
        Properties properties = new Properties();
        properties.put("user", "");
        properties.put("password", "");
        String connection = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        try {
            return DriverManager.getConnection(connection, properties);
        } catch (SQLException e) {
            throw new RuntimeException("Can't established connection to DB", e);
        }
    }
}