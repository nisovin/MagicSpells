package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;

public final class OldMultiSpell extends Spell {

	private boolean castWithItem;
	private boolean castByCommand;
	private boolean checkIndividualCooldowns;
	
	private ArrayList<Action> actions;
	
	public OldMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// reload config stuff, but from multispells section instead
		loadConfigData(config, spellName, "multispells");
		
		castWithItem = config.getBoolean("multispells." + spellName + ".can-cast-with-item", true);
		castByCommand = config.getBoolean("multispells." + spellName + ".can-cast-by-command", true);
		checkIndividualCooldowns = config.getBoolean("multispells." + spellName + ".check-individual-cooldowns", false);

		actions = new ArrayList<Action>();
		List<String> spellList = config.getStringList("multispells." + spellName + ".spells", null);
		if (spellList != null) {
			for (String s : spellList) {
				if (s.matches("DELAY [0-9]+")) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new Action(delay));
				} else {
					Spell spell = MagicSpells.getSpellByInternalName(s);
					if (spell != null) {
						actions.add(new Action(spell));
					} else {
						Bukkit.getServer().getLogger().severe("MagicSpells: no such spell '" + s + "' for multi-spell '" + spellName + "'");
					}
				}
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// check cooldowns
			if (checkIndividualCooldowns) {
				for (Action action : actions) {
					if (action.isSpell()) {
						if (action.getSpell().onCooldown(player)) {
							// a spell is on cooldown
							sendMessage(player, MagicSpells.strOnCooldown);
							return PostCastAction.ALREADY_HANDLED;
						}
					}
				}
			}
			
			int delay = 0;
			Spell spell;
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
				} else if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						spell.castSpell(player, SpellCastState.NORMAL, power, null);
					} else {
						Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedSpell(spell, player, power), delay);
					}
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}
	
	private class Action {
		private Spell spell;
		private int delay;
		
		public Action(Spell spell) {
			this.spell = spell;
			this.delay = 0;
		}
		
		public Action(int delay) {
			this.delay = delay;
			this.spell = null;
		}
		
		public boolean isSpell() {
			return spell != null;
		}
		
		public Spell getSpell() {
			return spell;
		}
		
		public boolean isDelay() {
			return delay > 0;
		}
		
		public int getDelay() {
			return delay;
		}
	}
	
	private class DelayedSpell implements Runnable {
		private Spell spell;
		private Player player;
		private float power;
		
		public DelayedSpell(Spell spell, Player player, float power) {
			this.spell = spell;
			this.player = player;
			this.power = power;
		}
		
		@Override
		public void run() {
			spell.castSpell(player, SpellCastState.NORMAL, power, null);
		}
	}

}
