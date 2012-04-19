package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;

public final class TargetedMultiSpell extends TargetedSpell {

	private boolean checkIndividualCooldowns;
	private boolean requireEntityTarget;
	private boolean targetPlayers;
	private boolean obeyLos;
	private String strNoTarget;
	
	private List<String> spellList;
	private ArrayList<Action> actions;
	
	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");

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
						if (spell instanceof TargetedEntitySpell || spell instanceof TargetedLocationSpell) {
							actions.add(new Action((TargetedSpell)spell));
						} else {
							MagicSpells.error("Invalid spell '" + s + "' for multi-spell '" + internalName + "'");
						}
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
			
			// get target
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				entTarget = getTargetedEntity(player, range, targetPlayers, obeyLos);
			} else {
				Block b = null;
				try {
					b = player.getTargetBlock(null, range);
					if (b != null && b.getType() != Material.AIR) {
						locTarget = b.getLocation();
					}
				} catch (IllegalStateException e) {
					b = null;
				}
			}
			if (locTarget == null && entTarget == null) {
				sendMessage(player, strNoTarget);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
			
			boolean somethingWasDone = false;
			int delay = 0;
			TargetedSpell spell;
			List<DelayedSpell> delayedSpells = new ArrayList<DelayedSpell>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
				} else if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						boolean ok = castTargetedSpell(spell, player, entTarget, locTarget, power);
						if (ok) {
							somethingWasDone = true;
						} else {
							// spell failed - exit loop
							break;
						}
					} else {
						DelayedSpell ds = new DelayedSpell(spell, player, entTarget, locTarget, power, delayedSpells);
						delayedSpells.add(ds);
						Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, ds, delay);
						somethingWasDone = true;
					}
				}
			}
			
			if (!somethingWasDone) {
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean castTargetedSpell(TargetedSpell spell, Player caster, LivingEntity entTarget, Location locTarget, float power) {
		if (spell instanceof TargetedEntitySpell && entTarget != null) {
			return ((TargetedEntitySpell)spell).castAtEntity(caster, entTarget, power);
		} else if (spell instanceof TargetedLocationSpell) {
			if (entTarget != null) {
				return ((TargetedLocationSpell)spell).castAtLocation(caster, entTarget.getLocation(), power);
			} else if (locTarget != null) {
				return ((TargetedLocationSpell)spell).castAtLocation(caster, locTarget, power);
			}
		}
		return false;
	}
	
	private class Action {
		private TargetedSpell spell;
		private int delay;
		
		public Action(TargetedSpell spell) {
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
		
		public TargetedSpell getSpell() {
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
		private TargetedSpell spell;
		private Player player;
		private LivingEntity entTarget;
		private Location locTarget;
		private float power;
		
		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;
		
		public DelayedSpell(TargetedSpell spell, Player player, LivingEntity entTarget, Location locTarget, float power, List<DelayedSpell> delayedSpells) {
			this.spell = spell;
			this.player = player;
			this.entTarget = entTarget;
			this.locTarget = locTarget;
			this.power = power;
			this.delayedSpells = delayedSpells;
			this.cancelled = false;
		}
		
		public void cancel() {
			cancelled = true;
			delayedSpells = null;
		}
		
		@Override
		public void run() {
			if (!cancelled) {
				boolean ok = castTargetedSpell(spell, player, entTarget, locTarget, power);
				if (ok) {
					delayedSpells.remove(this);
				} else {
					for (DelayedSpell ds : delayedSpells) {
						ds.cancel();
					}
					delayedSpells.clear();
				}
			}
			delayedSpells = null;
		}
	}
}
