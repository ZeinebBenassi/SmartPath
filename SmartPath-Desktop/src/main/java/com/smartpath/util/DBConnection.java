package com.smartpath.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static Connection connection;
    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/smartpath?serverTimezone=UTC&useSSL=false";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    public static Connection getInstance() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found!", e);
            }
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}