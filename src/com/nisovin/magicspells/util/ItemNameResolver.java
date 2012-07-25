package com.nisovin.magicspells.util;

public interface ItemNameResolver {

	public ItemTypeAndData resolve(String string);
	
	public class ItemTypeAndData {
		public int id = 0;
		public short data = 0;
	}
	
}
