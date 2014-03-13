package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class DummySpell extends BuffSpell {

	private Set<String> players = new HashSet<String>();
	
	public DummySpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		players.add(player.getName());
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return players.contains(player.getName());
	}
	
	@Override
	public void turnOffBuff(Player player) {
		players.remove(player.getName());
	}

	@Override
	protected void turnOff() {
		players.clear();
	}

}
