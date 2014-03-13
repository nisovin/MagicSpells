package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class PlayerOnlineCondition extends Condition {
	
	String name;
	
	@Override
	public boolean setVar(String var) {
		name = var;
		return true;
	}
	
	@Override
	public boolean check(Player player) {
		return Bukkit.getPlayerExact(name) != null;
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player);
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return check(player);
	}

}
