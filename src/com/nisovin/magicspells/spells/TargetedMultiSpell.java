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
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private boolean checkIndividualCooldowns;
	private boolean requireEntityTarget;
	private boolean pointBlank;
	private int yOffset;
	private boolean castRandomSpellInstead;
	private boolean stopOnFail;
	
	private List<String> spellList;
	private ArrayList<Action> actions;
	private Random random = new Random();
	
	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		pointBlank = getConfigBoolean("point-blank", false);
		yOffset = getConfigInt("y-offset", 0);
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
			
			// get target
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(player, power);
				if (info != null) {
					entTarget = info.getTarget();
					power = info.getPower();
				}
			} else if (pointBlank) {
				locTarget = player.getLocation();
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
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset);
			}
			
			boolean somethingWasDone = runSpells(player, entTarget, locTarget, power);
			
			if (!somethingWasDone) {
				return noTarget(player);
			}
			
			if (entTarget != null) {
				sendMessages(player, entTarget);
				return PostCastAction.NO_MESSAGES;
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
		return runSpells(null, null, location, power);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return runSpells(caster, target, null, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return runSpells(null, target, null, power);
	}
	
	boolean runSpells(Player player, LivingEntity entTarget, Location locTarget, float power) {
		boolean somethingWasDone = false;
		if (!castRandomSpellInstead) {
			int delay = 0;
			Subspell spell;
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
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) {
				somethingWasDone = castTargetedSpell(action.getSpell(), player, entTarget, locTarget, power);
			} else {
				somethingWasDone = false;
			}
		}
		if (somethingWasDone) {
			if (player != null) {
				if (entTarget != null) {
					playSpellEffects(player, entTarget);
				} else if (locTarget != null) {
					playSpellEffects(player, locTarget);
				}
			} else {
				if (entTarget != null) {
					playSpellEffects(EffectPosition.TARGET, entTarget);
				} else if (locTarget != null) {
					playSpellEffects(EffectPosition.TARGET, locTarget);
				}
			}
		}
		return somethingWasDone;
	}
	
	private boolean castTargetedSpell(Subspell spell, Player caster, LivingEntity entTarget, Location locTarget, float power) {
		boolean success = false;
		if (spell.isTargetedEntitySpell() && entTarget != null) {
			success = spell.castAtEntity(caster, entTarget, power);
		} else if (spell.isTargetedLocationSpell()) {
			if (entTarget != null) {
				success = spell.castAtLocation(caster, entTarget.getLocation(), power);
			} else if (locTarget != null) {
				success = spell.castAtLocation(caster, locTarget, power);
			}
		} else {
			success = spell.cast(caster, power) == PostCastAction.HANDLE_NORMALLY;
		}
		return success;
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
		private Player player;
		private LivingEntity entTarget;
		private Location locTarget;
		private float power;
		
		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;
		
		public DelayedSpell(Subspell spell, Player player, LivingEntity entTarget, Location locTarget, float power, List<DelayedSpell> delayedSpells) {
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
				if (player == null || player.isValid()) {
					boolean ok = castTargetedSpell(spell, player, entTarget, locTarget, power);
					delayedSpells.remove(this);
					if (!ok && stopOnFail) {
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
