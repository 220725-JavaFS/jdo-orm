package com.revature.exceptions;

public class JsonMappingException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public JsonMappingException() {}
	
	public JsonMappingException(String message) {
		super(message);
	}
	
	public JsonMappingException(String message, Exception e) {
		super(message, e);
	}
}
