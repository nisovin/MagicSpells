package com.nisovin.magicspells.spells.instant;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class TimeSpell extends InstantSpell {

	private int timeToSet;
	private String strAnnounce;
		
	public TimeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		timeToSet = getConfigInt("time-to-set", 0);
		strAnnounce = getConfigString("str-announce", "The sun suddenly appears in the sky.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			World world = player.getWorld();
			world.setTime(timeToSet);
			for (Player p : world.getPlayers()) {
				sendMessage(p, strAnnounce);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
