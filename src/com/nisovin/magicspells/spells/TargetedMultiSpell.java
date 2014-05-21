package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private boolean checkIndividualCooldowns;
	private boolean checkIndividualModifiers;
	private boolean showIndividualMessages;
	private boolean requireEntityTarget;
	private boolean castRandomSpellInstead;
	private boolean stopOnFail;
	
	private List<String> spellList;
	private ArrayList<Action> actions;
	private Random random = new Random();
	
	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", false);
		checkIndividualModifiers = getConfigBoolean("check-individual-modifiers", false);
		showIndividualMessages = getConfigBoolean("show-individual-messages", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
		stopOnFail = getConfigBoolean("stop-on-fail", true);

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
				TargetInfo<LivingEntity> info = getTargetedEntity(player, power);
				if (info != null) {
					entTarget = info.getTarget();
					power = info.getPower();
				}
			} else {
				Block b = null;
				try {
					b = getTargetedBlock(player, power);
					if (b != null && b.getType() != Material.AIR) {
						locTarget = b.getLocation();
					}
				} catch (IllegalStateException e) {
					b = null;
				}
			}
			if (locTarget == null && entTarget == null) {
				return noTarget(player);
			}
			
			boolean somethingWasDone = runSpells(player, entTarget, locTarget, power);
			
			if (!somethingWasDone) {
				return noTarget(player);
			}
			
			if (entTarget != null) {
				sendMessages(player, entTarget);
				playSpellEffects(player, entTarget);
				return PostCastAction.NO_MESSAGES;
			} else if (locTarget != null) {
				playSpellEffects(player, locTarget);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return runSpells(caster, null, target, power);
	}
	
	@Override
	public boolean castAtLocation(Location location, float power) {
		return false;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return runSpells(caster, target, null, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	boolean runSpells(Player player, LivingEntity entTarget, Location locTarget, float power) {
		if (!castRandomSpellInstead) {
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
							if (stopOnFail) {
								break;
							} else {
								continue;
							}
						}
					} else {
						DelayedSpell ds = new DelayedSpell(spell, player, entTarget, locTarget, power, delayedSpells);
						delayedSpells.add(ds);
						Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, ds, delay);
						somethingWasDone = true;
					}
				}
			}
			return somethingWasDone;
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) {
				castTargetedSpell(action.getSpell(), player, entTarget, locTarget, power);
				return true;
			} else {
				return false;
			}
		}
	}
	
	private boolean castTargetedSpell(TargetedSpell spell, Player caster, LivingEntity entTarget, Location locTarget, float power) {
		boolean success = false;
		if (checkIndividualModifiers) {
			ModifierSet castModifiers = spell.getModifiers();
			if (castModifiers != null) {
				SpellCastEvent event = new SpellCastEvent(spell, caster, SpellCastState.NORMAL, power, null, 0, null, 0);
				castModifiers.apply(event);
				if (event.isCancelled()) {
					return false;
				}
				power = event.getPower();
			}
			ModifierSet targetModifers = spell.getTargetModifiers();
			if (targetModifers != null) {
				if (entTarget != null) {
					SpellTargetEvent event = new SpellTargetEvent(spell, caster, entTarget, power);
					targetModifiers.apply(event);
					if (event.isCancelled()) {
						return false;
					}
					entTarget = event.getTarget();
					power = event.getPower();
				} else if (locTarget != null) {
					SpellTargetLocationEvent event = new SpellTargetLocationEvent(spell, caster, locTarget, power);
					targetModifiers.apply(event);
					if (event.isCancelled()) {
						return false;
					}
					locTarget = event.getTargetLocation();
					power = event.getPower();
				}
			}
		}
		if (spell instanceof TargetedEntitySpell && entTarget != null) {
			success = ((TargetedEntitySpell)spell).castAtEntity(caster, entTarget, power);
		} else if (spell instanceof TargetedLocationSpell) {
			if (entTarget != null) {
				success = ((TargetedLocationSpell)spell).castAtLocation(caster, entTarget.getLocation(), power);
			} else if (locTarget != null) {
				success = ((TargetedLocationSpell)spell).castAtLocation(caster, locTarget, power);
			}
		}
		if (success && showIndividualMessages) {
			if (entTarget != null) {
				spell.sendMessages(caster, entTarget);
			} else {
				spell.sendMessages(caster);
			}
		}
		return success;
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
		
		public void cancelAll() {
			for (DelayedSpell ds : delayedSpells) {
				if (ds != this) {
					ds.cancel();
				}
			}
			delayedSpells.clear();
			cancel();
		}
		
		@Override
		public void run() {
			if (!cancelled) {
				if (player.isValid()) {
					boolean ok = castTargetedSpell(spell, player, entTarget, locTarget, power);
					if (ok) {
						delayedSpells.remove(this);
					} else {
						cancelAll();
					}
				} else {
					cancelAll();
				}
			}
			delayedSpells = null;
		}
	}
}
