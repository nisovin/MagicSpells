package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class AreaEffectSpell extends TargetedLocationSpell {

	private int radius;
	private int verticalRadius;
	private boolean pointBlank;
	private boolean failIfNoTargets;
	private boolean targetPlayers;
	private List<String> spellNames;
	private List<TargetedSpell> spells;
	
	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("horizontal-radius", 10);
		verticalRadius = getConfigInt("vertical-radius", 5);
		pointBlank = getConfigBoolean("point-blank", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		targetPlayers = getConfigBoolean("target-players", false);
		spellNames = getConfigStringList("spells", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		spells = new ArrayList<TargetedSpell>();
		
		if (spellNames != null && spellNames.size() > 0) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) {
					if (spell instanceof TargetedLocationSpell || spell instanceof TargetedEntitySpell) {
						spells.add((TargetedSpell)spell);
					} else {
						MagicSpells.error("AreaEffect spell '" + name + "' attempted to use non-targeted spell '" + spellName + "'");
					}
				} else {
					MagicSpells.error("AreaEffect spell '" + name + "' attempted to use invalid spell '" + spellName + "'");
				}
			}
			spellNames.clear();
			spellNames = null;
		}
		
		if (spells.size() == 0) {
			MagicSpells.error("AreaEffect spell '" + name + "' has no spells!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			
			// get location for aoe
			Location loc = null;
			if (pointBlank) {
				loc = player.getLocation();
			} else {
				try {
					Block block = player.getTargetBlock(null, range);
					if (block != null && block.getType() != Material.AIR) {
						loc = block.getLocation();
					}
				} catch (IllegalStateException e) {
					loc = null;
				}
			}
			if (loc == null) {
				return noTarget(player);
			}
			
			// cast spells on nearby entities
			boolean done = doAoe(player, loc, power);
			
			// check if no targets
			if (!done && failIfNoTargets) {
				return noTarget(player);
			}
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean doAoe(Player player, Location location, float power) {
		int count = 0;

		Entity center = location.getWorld().dropItemNaturally(location, new ItemStack(1, 0));
		
		List<Entity> nearbyEntities = center.getNearbyEntities(radius, verticalRadius, radius);
		for (Entity e : nearbyEntities) {
			if (e instanceof LivingEntity && !((LivingEntity)e).isDead() && !e.equals(player) && (targetPlayers || !(e instanceof Player))) {
				LivingEntity target = (LivingEntity)e;
				SpellTargetEvent event = new SpellTargetEvent(this, player, target);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					continue;
				} else {
					target = event.getTarget();
				}
				for (TargetedSpell spell : spells) {
					if (spell instanceof TargetedEntitySpell) {
						((TargetedEntitySpell)spell).castAtEntity(player, target, power);
						playGraphicalEffects(player, target);
					} else if (spell instanceof TargetedLocationSpell) {
						((TargetedLocationSpell)spell).castAtLocation(player, target.getLocation(), power);
						playGraphicalEffects(player, target);
					}
				}
				count++;
			}
		}
		
		center.remove();
		
		return count > 0;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	
	
}
