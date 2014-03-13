package com.nisovin.magicspells.materials;

import java.util.Random;


public interface ItemNameResolver {

	static Random rand = new Random();

	@Deprecated
	public ItemTypeAndData resolve(String string);
	
	public MagicMaterial resolveItem(String string);
	
	public MagicMaterial resolveBlock(String string);
	
	public class ItemTypeAndData {
		public int id = 0;
		public short data = 0;
	}
	
}
