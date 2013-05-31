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

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ForcebombSpell extends TargetedLocationSpell {

	private int radiusSquared;
	private boolean targetPlayers;
	private boolean dontPushCaster;
	private int force;
	private int yForce;
	private int maxYForce;
	private boolean callTargetEvents;
	
	public ForcebombSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radiusSquared = getConfigInt("radius", 3);
		radiusSquared *= radiusSquared;
		targetPlayers = getConfigBoolean("target-players", false);
		dontPushCaster = getConfigBoolean("dont-push-caster", true);
		force = getConfigInt("pushback-force", 30);
		yForce = getConfigInt("additional-vertical-force", 15);
		maxYForce = getConfigInt("max-vertical-force", 20);
		callTargetEvents = getConfigBoolean("call-target-events", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = player.getTargetBlock(MagicSpells.getTransparentBlocks(), range);
			if (block != null && block.getType() != Material.AIR) {
				knockback(player, block.getLocation(), power);
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
	
	public void knockback(Player player, Location location, float power) {
	    Vector t = location.toVector();
		Collection<Entity> entities = location.getWorld().getEntitiesByClasses(LivingEntity.class);
		Vector e, v;
		for (Entity entity : entities) {
			if ((targetPlayers || !(entity instanceof Player)) && (!dontPushCaster || !entity.equals(player)) && entity.getLocation().distanceSquared(location) <= radiusSquared) {
				if (callTargetEvents) {
					SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity)entity);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						continue;
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
		playSpellEffects(EffectPosition.CASTER, player);
	}

}
