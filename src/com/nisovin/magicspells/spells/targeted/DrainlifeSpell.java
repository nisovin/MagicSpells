package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.ExperienceUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;

public class DrainlifeSpell extends TargetedEntitySpell {
	
	private String takeType;
	private int takeAmt;
	private String giveType;
	private int giveAmt;
	private boolean showSpellEffect;
	private int animationSpeed;
	private boolean instant;
	private boolean ignoreArmor;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	
	public DrainlifeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		takeType = getConfigString("take-type", "health");
		takeAmt = getConfigInt("take-amt", 2);
		giveType = getConfigString("give-type", "health");
		giveAmt = getConfigInt("give-amt", 2);
		showSpellEffect = getConfigBoolean("show-spell-effect", true);
		animationSpeed = getConfigInt("animation-speed", 2);
		instant = getConfigBoolean("instant", true);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail: no target
				return noTarget(player);
			} else {
				boolean drained = drain(player, target, power);
				if (!drained) {
					return noTarget(player);
				} else {
					sendMessages(player, target);
					return PostCastAction.NO_MESSAGES;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean drain(Player player, LivingEntity target, float power) {
		int take = Math.round(takeAmt*power);
		int give = Math.round(giveAmt*power);
		
		// drain from target
		if (takeType.equals("health")) {
			if (target instanceof Player && checkPlugins) {
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, take);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return false;
				}
				take = event.getDamage();
			}
			if (ignoreArmor) {
				int health = target.getHealth() - take;
				if (health < 0) health = 0;
				target.setHealth(health);
				target.playEffect(EntityEffect.HURT);
			} else {
				target.damage(take);
			}
		} else if (takeType.equals("mana")) {
			if (target instanceof Player) {
				boolean removed = MagicSpells.getManaHandler().removeMana((Player)target, take, ManaChangeReason.OTHER);
				if (!removed) {
					give = 0;
				}
			}
		} else if (takeType.equals("hunger")) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int food = p.getFoodLevel();
				if (give > food) give = food;
				food -= take;
				if (food < 0) food = 0;
				p.setFoodLevel(food);
			}
		} else if (takeType.equals("experience")) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int exp = ExperienceUtils.getCurrentExp(p);
				if (give > exp) give = exp;
				ExperienceUtils.changeExp(p, -take);
			}
		}
		
		// give to caster
		if (instant) {
			giveToCaster(player, give);
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}		
		
		// show animation
		if (showSpellEffect) {
			new DrainlifeAnim(target.getLocation(), player, give);
		}
		
		return true;
	}
	
	private void giveToCaster(Player player, int give) {
		if (giveType.equals("health")) {
			int h = player.getHealth()+Math.round(give);
			if (h>20) h=20;
			player.setHealth(h);
		} else if (giveType.equals("mana")) {
			MagicSpells.getManaHandler().addMana(player, give, ManaChangeReason.OTHER);
		} else if (takeType.equals("hunger")) {
			int food = player.getFoodLevel();
			food += give;
			if (food > 20) food = 20;
			player.setFoodLevel(food);
		} else if (takeType.equals("experience")) {
			ExperienceUtils.changeExp(player, give);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			return drain(caster, target, power);
		}
	}
	
	private class DrainlifeAnim extends SpellAnimation {
		
		Vector current;
		Player caster;
		World world;
		int giveAmt;
		
		public DrainlifeAnim(Location start, Player caster, int giveAmt) {
			super(animationSpeed, true);
			
			this.current = start.toVector();
			this.caster = caster;
			this.world = caster.getWorld();
			this.giveAmt = giveAmt;
		}

		@Override
		protected void onTick(int tick) {
			Vector targetVector = caster.getLocation().toVector();
			Vector tempVector = current.clone();
			tempVector.subtract(caster.getLocation().toVector()).normalize();
			current.subtract(tempVector);
			world.playEffect(current.toLocation(world), Effect.SMOKE, 4);
			if (current.distanceSquared(targetVector) < 4 || tick > range * 1.5) {
				stop();
				if (!instant) {
					giveToCaster(caster, giveAmt);
					playSpellEffects(EffectPosition.CASTER, caster);
				}
			}
		}
	}

}
