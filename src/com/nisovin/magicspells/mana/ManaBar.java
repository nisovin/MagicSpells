package com.nisovin.magicspells.mana;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.events.ManaChangeEvent;

public class ManaBar {

	private Player player;
	private String playerName;
	private int maxMana;
	private int regenAmount;
	
	private int mana;
	
	public ManaBar(Player player, int maxMana, int regenAmount) {
		this.player = player;
		this.playerName = player.getName().toLowerCase();
		this.maxMana = maxMana;
		this.regenAmount = regenAmount;
		
		this.mana = maxMana;
	}
	
	public Player getPlayer() {
		if (player != null && player.isOnline()) {
			return player;
		}
		return Bukkit.getPlayerExact(playerName);
	}
	
	public int getMana() {
		return mana;
	}
	
	public int getMaxMana() {
		return maxMana;
	}
	
	public int getRegenAmount() {
		return regenAmount;
	}
	
	public void setMaxMana(int max) {
		this.maxMana = max;
	}
	
	public void setRegenAmount(int amount) {
		this.regenAmount = amount;
	}
	
	public boolean has(int amount) {
		return mana >= amount;
	}
	
	public boolean changeMana(int amount, ManaChangeReason reason) {
		int newAmt = mana;
		
		if (amount > 0) {
			if (mana == maxMana) return false;
			newAmt += amount;
			if (newAmt > maxMana) newAmt = maxMana;
		} else if (amount < 0) {
			if (mana == 0) return false;
			newAmt += amount;
			if (newAmt < 0) newAmt = 0;
		}
		if (newAmt == mana) return false;
		
		newAmt = callManaChangeEvent(newAmt, reason);
		if (newAmt == mana) return false;
		mana = newAmt;
		return true;
	}
	
	public boolean regenerate() {
		if ((regenAmount > 0 && mana == maxMana) || (regenAmount < 0 && mana == 0)) return false;
		return changeMana(regenAmount, ManaChangeReason.REGEN);
	}
	
	private int callManaChangeEvent(int newAmt, ManaChangeReason reason) {
		Player player = getPlayer();
		if (player != null && player.isOnline()) {
			ManaChangeEvent event = new ManaChangeEvent(player, mana, newAmt, maxMana, reason);
			Bukkit.getPluginManager().callEvent(event);
			return event.getNewAmount();
		} else {
			return newAmt;
		}
	}
	
}
