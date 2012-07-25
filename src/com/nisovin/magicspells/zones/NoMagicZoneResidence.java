package com.nisovin.magicspells.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.nisovin.magicspells.MagicSpells;

public class NoMagicZoneResidence extends NoMagicZone {

	private String regionName;
	
	@Override
	public void initialize(ConfigurationSection config) {
		this.regionName = config.getString("region", "");
	}

	@Override
	public boolean inZone(Location location) {
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("Residence")) {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(location);
			if (res != null) {
				if (res.getName().equalsIgnoreCase(regionName)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			MagicSpells.error("Failed to access Residence region '" + regionName + "'");
			return false;
		}
	}

}
