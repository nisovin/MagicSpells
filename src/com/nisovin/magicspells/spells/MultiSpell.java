package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;

public final class MultiSpell extends InstantSpell {

	private boolean castWithItem;
	private boolean castByCommand;
	private boolean checkIndividualCooldowns;
	private boolean checkIndividualModifiers;
	private boolean showIndividualMessages;
	private boolean castRandomSpellInstead;
	private boolean fakeCastIndividualSpells;
	
	private List<String> spellList;
	private ArrayList<Action> actions;
	private Random random = new Random();
	
	public MultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", false);
		checkIndividualModifiers = getConfigBoolean("check-individual-modifiers", false);
		showIndividualMessages = getConfigBoolean("show-individual-messages", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
		fakeCastIndividualSpells = getConfigBoolean("fake-cast-individual-spells", false);

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
					Spell spell = MagicSpells.getSpellByInternalName(s);
					if (spell != null) {
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
							if (action.getSpell().onCooldown(player)) {
								// a spell is on cooldown
								sendMessage(player, strOnCooldown);
								return PostCastAction.ALREADY_HANDLED;
							}
						}
					}
				}
				
				// cast/schedule spells
				int delay = 0;
				Spell spell;
				for (Action action : actions) {
					if (action.isDelay()) {
						delay += action.getDelay();
					} else if (action.isSpell()) {
						spell = action.getSpell();
						if (delay == 0) {
							castSpell(player, spell, power);
						} else {
							Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedSpell(spell, player, power), delay);
						}
					}
				}
			} else {
				// random spell
				Action action = actions.get(random.nextInt(actions.size()));
				if (action.isSpell()) {
					// first check cooldown
					if (checkIndividualCooldowns && action.getSpell().onCooldown(player)) {
						sendMessage(player, strOnCooldown);
						return PostCastAction.ALREADY_HANDLED;
					}
					// cast the spell
					castSpell(player, action.getSpell(), power);
				}
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void castSpell(Player player, Spell spell, float power) {
		if (fakeCastIndividualSpells) {
			SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power, null, cooldown, reagents.clone(), castTime);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			} else {
				power = event.getPower();
			}
		} else if (checkIndividualModifiers) {
			ModifierSet castModifiers = spell.getModifiers();
			if (castModifiers != null) {
				SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power, null, 0, null, 0);
				castModifiers.apply(event);
				if (event.isCancelled()) {
					return;
				}
				power = event.getPower();
			}
		}
		PostCastAction act = spell.castSpell(player, SpellCastState.NORMAL, power, null);
		if (showIndividualMessages && act.sendMessages()) {
			spell.sendMessages(player);
		}
		if (fakeCastIndividualSpells) {
			Bukkit.getPluginManager().callEvent(new SpellCastedEvent(spell, player, SpellCastState.NORMAL, power, null, cooldown, reagents.clone(), act));
		}
	}
	
	@Override
	public boolean castFromConsole(final CommandSender sender, final String[] args) {
		if (!castRandomSpellInstead) {
			int delay = 0;
			for (Action action : actions) {
				if (action.isSpell()) {
					if (delay == 0) {
						action.getSpell().castFromConsole(sender, args);
					} else {
						final Spell spell = action.getSpell();
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
				action.getSpell().castFromConsole(sender, args);
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
		private String playerName;
		private float power;
		
		public DelayedSpell(Spell spell, Player player, float power) {
			this.spell = spell;
			this.playerName = player.getName();
			this.power = power;
		}
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null && player.isValid()) {
				castSpell(player, spell, power);
			}
		}
	}

}
