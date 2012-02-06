package com.nisovin.magicspells;

import org.bukkit.entity.Player;

public abstract class ManaHandler {

	public abstract void createManaBar(Player player);
	
	public abstract boolean hasMana(Player player, int amount);
	
	public abstract boolean removeMana(Player player, int amount);
	
	public abstract boolean addMana(Player player, int amount);
	
	public void showMana(Player player) {
		showMana(player, false);
	}
	
	public abstract void showMana(Player player, boolean forceShowInChat);
	
	public abstract void turnOff();
	
}
