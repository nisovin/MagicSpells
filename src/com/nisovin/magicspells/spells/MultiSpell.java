package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;

public final class MultiSpell extends InstantSpell {

	private boolean castWithItem;
	private boolean castByCommand;
	private boolean checkIndividualCooldowns;
	private boolean castRandomSpellInstead;
	
	private List<String> spellList;
	private ArrayList<Action> actions;
	private Random random = new Random();
	
	public MultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);

		actions = new ArrayList<Action>();
		spellList = getConfigStringList("spells", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellList != null) {
			for (String s : spellList) {
				if (s.matches("DELAY [0-9]+")) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new Action(delay));
				} else {
					Subspell spell = new Subspell(s);
					if (spell.process()) {
						actions.add(new Action(spell));
					} else {
						MagicSpells.error("No such spell '" + s + "' for multi-spell '" + internalName + "'");
					}
				}
			}
		}
		spellList = null;
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (!castRandomSpellInstead) {
				// check cooldowns
				if (checkIndividualCooldowns) {
					for (Action action : actions) {
						if (action.isSpell()) {
							if (action.getSpell().getSpell().onCooldown(player)) {
								// a spell is on cooldown
								sendMessage(player, strOnCooldown);
								return PostCastAction.ALREADY_HANDLED;
							}
						}
					}
				}
				
				// cast/schedule spells
				int delay = 0;
				Subspell spell;
				for (Action action : actions) {
					if (action.isDelay()) {
						delay += action.getDelay();
					} else if (action.isSpell()) {
						spell = action.getSpell();
						if (delay == 0) {
							spell.cast(player, power);
						} else {
							Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedSpell(spell, player, power), delay);
						}
					}
				}
			} else {
				// random spell
				List<Action> list;
				if (checkIndividualCooldowns) {
					list = new ArrayList<Action>();
					for (Action a : actions) {
						if (a.isSpell() && !a.getSpell().getSpell().onCooldown(player)) {
							list.add(a);
						}
					}
				} else {
					list = actions;
				}
				if (list.size() > 0) {
					Action action = list.get(random.nextInt(list.size()));
					if (action.isSpell()) {
						action.getSpell().cast(player, power);
					}
				} else {
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(final CommandSender sender, final String[] args) {
		if (!castRandomSpellInstead) {
			int delay = 0;
			for (Action action : actions) {
				if (action.isSpell()) {
					if (delay == 0) {
						action.getSpell().getSpell().castFromConsole(sender, args);
					} else {
						final Spell spell = action.getSpell().getSpell();
						MagicSpells.scheduleDelayedTask(new Runnable() {
							public void run() {
								spell.castFromConsole(sender, args);
							}
						}, delay);
					}
				} else if (action.isDelay()) {
					delay += action.getDelay();
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) {
				action.getSpell().getSpell().castFromConsole(sender, args);
			}
		}
		return true;
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
		private Subspell spell;
		private int delay;
		
		public Action(Subspell spell) {
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
		
		public Subspell getSpell() {
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
		private Subspell spell;
		private String playerName;
		private float power;
		
		public DelayedSpell(Subspell spell, Player player, float power) {
			this.spell = spell;
			this.playerName = player.getName();
			this.power = power;
		}
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null && player.isValid()) {
				spell.cast(player, power);
			}
		}
	}

}
