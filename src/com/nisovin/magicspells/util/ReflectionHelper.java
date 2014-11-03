package com.nisovin.magicspells.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper<E> {

	Map<String, Field> fields = new HashMap<String, Field>();
	
	public ReflectionHelper(Class<? extends E> type, String... fields) {
		for (String fieldName : fields) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				this.fields.put(fieldName, field);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int getInt(E object, String field) {
		try {
			return fields.get(field).getInt(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setInt(E object, String field, int val) {
		try {
			fields.get(field).setInt(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public byte getByte(E object, String field) {
		try {
			return fields.get(field).getByte(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setByte(E object, String field, byte val) {
		try {
			fields.get(field).setByte(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public String getString(E object, String field) {
		try {
			return (String)fields.get(field).get(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setString(E object, String field, String val) {
		try {
			fields.get(field).set(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public Object get(E object, String field) {
		try {
			return fields.get(field).get(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void set(E object, String field, Object val) {
		try {
			fields.get(field).set(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
}
