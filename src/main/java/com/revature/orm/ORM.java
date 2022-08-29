package com.revature.orm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revature.exceptions.JsonMappingException;
import com.revature.utils.ConnectionUtil;


public class ORM {
	
	
	private static Logger log = LoggerFactory.getLogger(ORM.class);
	
	
	/**
	 * Creates and returns an object
	 * @param o
	 * @return 
	 */
	public <T> Object createObject(Object o) {
		
		// Creates a string builder for the sql statement
		StringBuilder build = new StringBuilder();
		build.append("INSERT INTO ");
		
		//Creates object class and retrieves class name
		Class<?> objectClass = o.getClass();
		String[] arr = objectClass.getName().split("\\.");	
		String className = arr[arr.length - 1];
		
		
		build.append(className + " (");

		// Creates fields of the class and appends the field names with the insert statement
		Field[] fields = objectClass.getDeclaredFields();
		
		for(Field f: fields) {
			f.getName();
			if(f.getName() == "id") continue;
			build.append(f.getName() + ", ");
		}
		
		build.delete(build.length() - 2, build.length());
		build.append(") VALUES (");
		
	
		//iterates the field values and appends them into the sql statement
		boolean num = false;
		for(Field f: fields) {
			if(f.getName() == "id") continue;
			String getterName = "get" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1);
			
			try {
				Method getterMethod = objectClass.getMethod(getterName);
				
				Object fieldValue = getterMethod.invoke(o);
				
				if (fieldValue.getClass() == String.class) {
					build.append("'" + fieldValue + "',");
				} else {
					build.append(fieldValue + ",");
					num = true;
				}
				
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}		
		}
		if(num) build.delete(build.length() - 1, build.length());
		if(!num) build.delete(build.length() - 2, build.length());
		
		build.append(");");
		
		String sql = build.toString();
		log.info(sql);
		
		try(Connection conn = ConnectionUtil.getConnection()) {
			
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.execute();
		} catch (SQLException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return o;
	}
	
	/**
	 * returns a list of objects
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public <T> List<T> readObjects(Class<T> clazz) {
			
		List<T> objects = new LinkedList<T>();
		
		StringBuilder build = new StringBuilder();
		
		Field[] fields = clazz.getDeclaredFields();
		

		build.append("SELECT * FROM ");
		
		String[] arr = clazz.getName().split("\\.");
		
		String className = arr[arr.length - 1];
		
		build.append(className);
		
		String sql = build.toString();
		log.info(sql);
		
		try(Connection conn = ConnectionUtil.getConnection()) {
			
			Statement statement = conn.createStatement(); 
			ResultSet result = statement.executeQuery(sql);
			
			while(result.next()) {
				
				try {
					T newObject = clazz.getDeclaredConstructor().newInstance();
					
					for(Field f: fields) {
						
						String nameField = f.getName();
						String setterName = "set" + nameField.substring(0, 1).toUpperCase() + nameField.substring(1);
						
						try {
							Class<?> setterParamType = clazz.getDeclaredField(nameField).getType();
							
							Method setter = clazz.getMethod(setterName, setterParamType);
							
							Object fieldValue = convertStringToFieldType(result.getString(nameField), setterParamType);
							
							setter.invoke(newObject, fieldValue);
							
						} catch (NoSuchFieldException e) {
							throw new JsonMappingException(nameField + " field does not exist in class" + clazz);
						} catch (NoSuchMethodException e) {
							throw new JsonMappingException("no valid setter for: " + nameField);
						} catch (IllegalAccessException e) {
							throw new JsonMappingException("cannot access setter for: " + nameField);
						} catch (InvocationTargetException | InstantiationException e) {
							throw new JsonMappingException("issue invoking setter for: " + nameField);
						}	
					}
					objects.add(newObject);
				
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					e.printStackTrace();
					System.out.println("lol");
				}			
			}
			return objects;
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		
		return null;
		
	}

	/**
	 * Update and returns an object
	 * @param <T>
	 * @param o
	 * @return
	 */
	public <T> Object updateObject(Object o) {
		StringBuilder build = new StringBuilder();
		
		build.append("UPDATE ");
		
		Class<?> objectClass = o.getClass();
		
		String[] arr = objectClass.getName().split("\\.");
		
		String className = arr[arr.length - 1];
		
		build.append(className + " SET ");
		
		Field[] fields = objectClass.getDeclaredFields();
		
		String id = "";
		
		for(Field f: fields) {
			String fieldName = f.getName();
			
			String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

			try {
				Method getterMethod = objectClass.getMethod(getterName);
				
				Object fieldValue = getterMethod.invoke(o);
				
				if(f.getName().contains("id")) { 
					id = fieldValue.toString();
					continue;
				}
				
				if (fieldValue.getClass() == String.class) {
					build.append(fieldName + " = '" + fieldValue + "',");
				} else {
					build.append(fieldName + " = " + fieldValue + ",");
				}
			}  catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		build.deleteCharAt(build.length() - 1);
		build.append(" WHERE id = " + id + ";");
		
		String sql = build.toString();
		log.info(sql);
		
		try(Connection conn = ConnectionUtil.getConnection()) {
			
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return o;
	}

	/**
	 * Deletes an object and returns a boolean to make sure it worked
	 * @param o
	 * @return
	 */
	public boolean deleteObject(Object o) {
		
		StringBuilder build = new StringBuilder();
		
		build.append("DELETE FROM ");
		
		Class<?> objectClass = o.getClass();
		
		String[] arr = objectClass.getName().split("\\.");
		
		String className = arr[arr.length - 1];
		
		build.append(className + " WHERE id = ");
		
		String getterName  = "getId";
		
		
			try {
				Method getterMethod = objectClass.getMethod(getterName);
				
				Object fieldValue = getterMethod.invoke(o);
				
				build.append(fieldValue + ";");
			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		String sql = build.toString();
		log.info(sql);
		
		try(Connection conn = ConnectionUtil.getConnection()) {
			
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.execute();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * A private method to convert string to field type
	 * @param input
	 * @param type
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Object convertStringToFieldType(String input, Class<?> type) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		switch(type.getName()) {
			case "byte":
				return Byte.valueOf(input);
			case "short":
				return Short.valueOf(input);
			case "int":
				return Integer.valueOf(input);
			case "long":
				return Long.valueOf(input);
			case "double":
				return Double.valueOf(input);
			case "float":
				return Float.valueOf(input);
			case "java.lang.String":
				return input;
			case "java.time.LocalDate":
				return LocalDate.parse(input);
			default:
				return type.getDeclaredConstructor().newInstance();
		}
			
	}
	
}
