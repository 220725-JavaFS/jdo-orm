package com.revature.utils;

import java.sql.Connection; //java.sql is the JDBC package
import java.sql.DriverManager;
import java.sql.SQLException;



public class ConnectionUtil {
	
	//
	private static Connection connection;
	
	public static Connection getConnection() throws SQLException {
		if(connection != null && !connection.isClosed()) {
			return connection;
		} else {
			//For many frameworks, or in cases where there are multiple SQL drivers, you will need to register which
			//Driver you are using for the connection interface. The class.forName method will allow you to do this
			try {
				Class.forName("org.postgresql.Driver");
			} catch(ClassNotFoundException e) {
				e.printStackTrace();
			}
			
			String url = "jdbc:postgresql://javafs220725.cnsxjswk0cie.us-east-1.rds.amazonaws.com:5432/postgres";
			String username = "postgres"; //It is possible to hide raw credentials by using ENV variables
			String password = "password"; // You can access those variables with System.getenv("var-name");
		
			connection = DriverManager.getConnection(url, username, password);
			return connection;
		}
	}
	
	public static void main(String[] args) {
		try {
			getConnection();
			System.out.println("Connection Successful");
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}
	
	
}

