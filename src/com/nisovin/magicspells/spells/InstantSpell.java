package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;

public abstract class InstantSpell extends Spell {
	
	private boolean castWithItem;
	private boolean castByCommand;
	
	public InstantSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
	}
	
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	public boolean canCastByCommand() {
		return castByCommand;
	}
}
