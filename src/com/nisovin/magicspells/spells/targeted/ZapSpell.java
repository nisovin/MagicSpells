package com.nisovin.magicspells.spells.targeted;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ZapSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private String strCantZap;
	private Set<Material> allowedBlockTypes;
	private Set<Material> disallowedBlockTypes;
	private boolean dropBlock;
	private boolean dropNormal;
	private boolean checkPlugins;
	private boolean playBreakEffect;
	
	public ZapSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strCantZap = getConfigString("str-cant-zap", "");
		
		List<String> allowed = getConfigStringList("allowed-block-types",null);
		if (allowed != null) {
			allowedBlockTypes = EnumSet.noneOf(Material.class);
			for (String s : allowed) {
				if (!s.isEmpty()) {
					MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
					if (m != null && m.getMaterial() != null) {
						allowedBlockTypes.add(m.getMaterial());
					}
				}
			}
		}
		
		List<String> disallowed = getConfigStringList("disallowed-block-types", Arrays.asList("bedrock", "lava", "stationary_lava", "water", "stationary_water"));
		if (disallowed != null) {
			disallowedBlockTypes = EnumSet.noneOf(Material.class);
			for (String s : disallowed) {
				if (!s.isEmpty()) {
					MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
					if (m != null && m.getMaterial() != null) {
						disallowedBlockTypes.add(m.getMaterial());
					}
				}
			}
		}
		
		dropBlock = getConfigBoolean("drop-block", false);
		dropNormal = getConfigBoolean("drop-normal", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("play-break-effect", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get targeted block
			Block target;
			try {
				target = getTargetedBlock(player, power);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					target = null;
				} else {
					target = event.getTargetLocation().getBlock();
				}
			}
			if (target != null) {
				// check for disallowed block
				if (!canZap(target)) {
					return noTarget(player, strCantZap);
				}
				// zap
				boolean ok = zap(target, player);
				if (!ok) {
					return noTarget(player, strCantZap);
				}
			} else {
				return noTarget(player, strCantZap);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean zap(Block target, Player player) {
		// check for protection
		if (checkPlugins && player != null) {
			BlockBreakEvent event = new BlockBreakEvent(target, player);
			MagicSpells.plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				// a plugin cancelled the event
				return false;
			}
		}
		
		// drop block
		if (dropBlock) {
			if (dropNormal) {
				target.breakNaturally();
			} else {
				target.getWorld().dropItemNaturally(target.getLocation(), target.getState().getData().toItemStack(1));
			}
		}
		
		// show animation
		if (playBreakEffect) {
			target.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
		}
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		}
		playSpellEffects(EffectPosition.TARGET, target.getLocation());
		if (player != null) {
			playSpellEffectsTrail(player.getLocation(), target.getLocation());
		}
		
		// remove block
		target.setType(Material.AIR);
		return true;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		if (canZap(block)) {
			zap(block, caster);
			return true;
		} else {
			Vector v = target.getDirection();
			block = target.clone().add(v).getBlock();
			if (canZap(block)) {
				zap(block, caster);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Block block = target.getBlock();
		if (canZap(block)) {
			zap(block, null);
			return true;
		}
		return false;
	}
	
	private boolean canZap(Block target) {
		return !(disallowedBlockTypes.contains(target.getType()) || (allowedBlockTypes.size() > 0 && !allowedBlockTypes.contains(target.getType())));
	}
}
