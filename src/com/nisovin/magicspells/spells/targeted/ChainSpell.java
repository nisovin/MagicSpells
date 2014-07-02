package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ChainSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	String spellNameToCast;
	Subspell spellToCast;
	ValidTargetChecker checker;
	int bounces;
	int bounceRange;
	int interval;
	boolean targetPlayers;
	boolean targetNonPlayers;
	
	public ChainSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellNameToCast = getConfigString("spell", "heal");
		bounces = getConfigInt("bounces", 3);
		bounceRange = getConfigInt("bounce-range", 8);
		interval = getConfigInt("interval", 10);
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		Subspell spell = new Subspell(spellNameToCast);
		if (spell.process()) {
			spellToCast = spell;
			checker = spell.getSpell().getValidTargetChecker();
		} else {
			MagicSpells.error("Invalid spell defined for ChainSpell '" + this.name + "'");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power, checker);
			if (target == null) {
				return noTarget(player);
			}
			chain(player, player.getLocation(), target.getTarget(), target.getPower());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void chain(Player player, Location start, LivingEntity target, float power) {
		List<LivingEntity> targets = new ArrayList<LivingEntity>();
		List<Float> targetPowers = new ArrayList<Float>();
		targets.add(target);
		targetPowers.add(power);
		
		// get targets
		LivingEntity current = target;
		int attempts = 0;
		while (targets.size() < bounces && attempts++ < bounces * 2) {
			List<Entity> entities = current.getNearbyEntities(bounceRange, bounceRange, bounceRange);
			for (Entity e : entities) {
				if (!(e instanceof LivingEntity)) {
					continue;
				}
				if (targets.contains(e)) {
					continue;
				}
				if (e instanceof Player) {
					if (!targetPlayers) {
						continue;
					}
				} else if (!targetNonPlayers) {
					continue;
				}
				if (checker != null && !checker.isValidTarget((LivingEntity)e)) {
					continue;
				}
				float thisPower = power;
				if (player != null) {
					SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity)e, thisPower);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						continue;
					} else {
						thisPower = event.getPower();
					}
				}
				
				targets.add((LivingEntity)e);
				targetPowers.add(thisPower);
				current = (LivingEntity)e;
				break;
			}
		}
		
		// cast spell at targets
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		} else if (start != null) {
			playSpellEffects(EffectPosition.CASTER, start);
		}
		if (interval <= 0) {
			for (int i = 0; i < targets.size(); i++) {
				Location from = null;
				if (i == 0) {
					from = start;
				} else {
					from = targets.get(i-1).getLocation();
				}
				castSpellAt(player, from, targets.get(i), targetPowers.get(i));
				if (i > 0) {
					playSpellEffectsTrail(targets.get(i-1).getLocation(), targets.get(i).getLocation());
				} else if (i == 0 && player != null) {
					playSpellEffectsTrail(player.getLocation(), targets.get(i).getLocation());
				}
				playSpellEffects(EffectPosition.TARGET, targets.get(i));
			}
		} else {
			new ChainBouncer(player, start, targets, power);
		}
	}
	
	private boolean castSpellAt(Player caster, Location from, LivingEntity target, float power) {
		if (spellToCast.isTargetedEntityFromLocationSpell() && from != null) {
			return spellToCast.castAtEntityFromLocation(caster, from, target, power);
		} else if (spellToCast.isTargetedEntitySpell()) {
			return spellToCast.castAtEntity(caster, target, power);
		} else if (spellToCast.isTargetedLocationSpell()) {
			return spellToCast.castAtLocation(caster, target.getLocation(), power);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		chain(caster, caster.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		chain(null, null, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		chain(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		chain(null, from, target, power);
		return true;
	}

	class ChainBouncer implements Runnable {
		Player caster;
		Location start;
		List<LivingEntity> targets;
		float power;
		int current = 0;
		int taskId;
		
		public ChainBouncer(Player caster, Location start, List<LivingEntity> targets, float power) {
			this.caster = caster;
			this.start = start;
			this.targets = targets;
			this.power = power;
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}
		
		public void run() {
			Location from = null;
			if (current == 0) {
				from = start;
			} else {
				from = targets.get(current-1).getLocation();
			}
			castSpellAt(caster, from, targets.get(current), power);
			if (current > 0) {
				playSpellEffectsTrail(targets.get(current-1).getLocation().add(0, .5, 0), targets.get(current).getLocation().add(0, .5, 0));
			} else if (current == 0 && caster != null) {
				playSpellEffectsTrail(caster.getLocation().add(0, .5, 0), targets.get(current).getLocation().add(0, .5, 0));
			}
			playSpellEffects(EffectPosition.TARGET, targets.get(current));
			current++;
			if (current >= targets.size()) {
				MagicSpells.cancelTask(taskId);
			}
		}
	}
	
}
