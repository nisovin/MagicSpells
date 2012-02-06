package com.nisovin.magicspells.spells.channeled;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.ChanneledSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SunSpell extends ChanneledSpell {

	private int timeToSet;
	private String strAnnounce;
		
	public SunSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		timeToSet = getConfigInt("time-to-set", 0);
		strAnnounce = getConfigString("str-announce", "The sun suddenly appears in the sky.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			String world = player.getWorld().getName();
			boolean success = addChanneler(world, player);
			if (!success) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	protected void finishSpell(String key, Location location) {
		World world = location.getWorld();
		world.setTime(timeToSet);
		for (Player p : world.getPlayers()) {
			sendMessage(p, strAnnounce);
		}
	}

}
