package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class LocationSpell extends InstantSpell {

	MagicLocation location;
	Subspell spell;
	
	public LocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String s = getConfigString("location", "world,0,0,0");
		try {
			String[] split = s.split(",");
			String world = split[0];
			double x = Double.parseDouble(split[1]);
			double y = Double.parseDouble(split[2]);
			double z = Double.parseDouble(split[3]);
			float yaw = 0;
			float pitch = 0;
			if (split.length > 4) {
				yaw = Float.parseFloat(split[4]);
			}
			if (split.length > 5) {
				pitch = Float.parseFloat(split[5]);
			}
			location = new MagicLocation(world, x, y, z, yaw, pitch);
		} catch (Exception e) {
			MagicSpells.error("Invalid location on LocationSpell '" + spellName + "'");
		}
		spell = new Subspell(getConfigString("spell", ""));
		if (spell == null) {
			MagicSpells.error("Invalid spell on LocationSpell '" + spellName + "'");
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (spell != null) {
			boolean ok = spell.process();
			if (!ok || !spell.isTargetedLocationSpell()) {
				MagicSpells.error("Invalid spell on LocationSpell '" + name + "'");
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = location.getLocation();
			if (loc != null) {
				spell.castAtLocation(player, loc, power);
				playSpellEffects(player, loc);
			} else {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
