package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class BiomeCondition extends Condition {

	Biome[] biomes;

	@Override
	public boolean setVar(String var) {
		String[] s = var.split(",");
		biomes = new Biome[s.length];
		for (int i = 0; i < s.length; i++) {
			biomes[i] = Biome.valueOf(s[i].toUpperCase());
			if (biomes[i] == null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player);
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}
	
	@Override
	public boolean check(Player player, Location location) {
		Biome biome = location.getBlock().getBiome();
		for (Biome b : biomes) {
			if (b == biome) {
				return true;
			}
		}
		return false;
	}
	
}
