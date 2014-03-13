package com.nisovin.magicspells.util;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MoneyHandler {
	
	private Economy economy;

	public MoneyHandler() {
		RegisteredServiceProvider<Economy> provider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (provider != null) {
			economy = provider.getProvider();
		}
	}
	
	public boolean hasMoney(Player player, float money) {
		if (economy == null) return false;
		return economy.has(player.getName(), money);
	}
	
	public void removeMoney(Player player, float money) {
		if (economy != null) {
			economy.withdrawPlayer(player.getName(), money);
		}
	}
	
	public void addMoney(Player player, float money) {
		if (economy != null) {
			economy.depositPlayer(player.getName(), money);
		}
	}
	
}
