package com.nisovin.magicspells.spells.targeted;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ForcebombSpell extends TargetedSpell implements TargetedLocationSpell {

	private int radiusSquared;
	private float yOffset;
	private int force;
	private int yForce;
	private int maxYForce;
	private boolean callTargetEvents;
	
	public ForcebombSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radiusSquared = getConfigInt("radius", 3);
		radiusSquared *= radiusSquared;
		yOffset = getConfigFloat("y-offset", 0);
		force = getConfigInt("pushback-force", 30);
		yForce = getConfigInt("additional-vertical-force", 15);
		maxYForce = getConfigInt("max-vertical-force", 20);
		callTargetEvents = getConfigBoolean("call-target-events", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(player, power);
			if (block != null && block.getType() != Material.AIR) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, block.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					block = null;
				} else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}
			if (block != null && block.getType() != Material.AIR) {
				knockback(player, block.getLocation().add(0.5, 0, 0.5), power);
			} else {
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		knockback(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		knockback(null, target, power);
		return true;
	}
	
	public void knockback(Player player, Location location, float basePower) {
		location = location.clone().add(0, yOffset, 0);
	    Vector t = location.toVector();
		Collection<Entity> entities = location.getWorld().getEntitiesByClasses(LivingEntity.class);
		Vector e, v;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity && (player == null || validTargetList.canTarget(player, (LivingEntity)entity)) && entity.getLocation().distanceSquared(location) <= radiusSquared) {
				float power = basePower;
				if (callTargetEvents && player != null) {
					SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity)entity, power);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						continue;
					} else {
						power = event.getPower();
					}
				}
				e = entity.getLocation().toVector();
				v = e.subtract(t).normalize().multiply(force/10.0*power);
				if (force != 0) {
					v.setY(v.getY() * (yForce/10.0*power));
				} else {
					v.setY(yForce/10.0*power);
				}
				if (v.getY() > (maxYForce/10.0)) {
					v.setY(maxYForce/10.0);
				}
				entity.setVelocity(v);
				playSpellEffects(EffectPosition.TARGET, entity);
			}
	    }
		playSpellEffects(EffectPosition.SPECIAL, location);
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		} else {
			playSpellEffects(EffectPosition.CASTER, location);
		}
	}

}
