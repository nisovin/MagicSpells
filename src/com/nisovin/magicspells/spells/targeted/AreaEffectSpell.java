package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class AreaEffectSpell extends TargetedLocationSpell {

	private int radiusSquared;
	private int verticalRadius;
	private boolean pointBlank;
	private boolean failIfNoTargets;
	private boolean targetPlayers;
	private boolean targetNonPlayers;
	private int maxTargets;
	private boolean beneficial;
	private List<String> spellNames;
	private List<TargetedSpell> spells;
	
	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radiusSquared = getConfigInt("horizontal-radius", 10);
		radiusSquared *= radiusSquared;
		verticalRadius = getConfigInt("vertical-radius", 5);
		pointBlank = getConfigBoolean("point-blank", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		targetPlayers = getConfigBoolean("target-players", false);
		targetNonPlayers = getConfigBoolean("target-non-players", true);
		maxTargets = getConfigInt("max-targets", 0);
		beneficial = getConfigBoolean("beneficial", false);
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
		
		Collection<Entity> entities = location.getWorld().getEntitiesByClasses(LivingEntity.class);
		for (Entity e : entities) {
			if (e.getLocation().distanceSquared(location) <= radiusSquared && Math.abs(e.getLocation().getY() - location.getY()) <= verticalRadius) {
				boolean isPlayer = (e instanceof Player);
				if (!((LivingEntity)e).isDead() && !(isPlayer && ((Player)e).getName().equals(player.getName())) && (targetPlayers || !isPlayer) && (targetNonPlayers || isPlayer)) {
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
							playSpellEffects(player, target);
						} else if (spell instanceof TargetedLocationSpell) {
							((TargetedLocationSpell)spell).castAtLocation(player, target.getLocation(), power);
							playSpellEffects(player, target);
						}
					}
					count++;
					if (maxTargets > 0 && count >= maxTargets) {
						break;
					}
				}
			}
		}
		
		return count > 0;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	@Override
	public boolean isBeneficial() {
		return beneficial;
	}
	
}
