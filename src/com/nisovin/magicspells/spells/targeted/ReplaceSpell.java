package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	int yOffset;
	int radiusUp;
	int radiusDown;
	int radiusHoriz;
	boolean pointBlank;
	boolean replaceRandom;
	boolean powerAffectsRadius;
	
	List<MagicMaterial> replace;
	List<MagicMaterial> replaceWith;

	Random random = new Random();
	
	public ReplaceSpell(MagicConfig config, String spellName) {
		super(config, spellName);
				
		yOffset = getConfigInt("y-offset", 0);
		radiusUp = getConfigInt("radius-up", 1);
		radiusDown = getConfigInt("radius-down", 1);
		radiusHoriz = getConfigInt("radius-horiz", 1);
		pointBlank = getConfigBoolean("point-blank", false);
		replaceRandom = getConfigBoolean("replace-random", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);
		
		replace = new ArrayList<MagicMaterial>();
		replaceWith = new ArrayList<MagicMaterial>();
		
		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			for (String s : list) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m != null) {
					replace.add(m);
				} else {
					MagicSpells.error("ReplaceSpell " + spellName + " invalid replace-blocks item: " + s);
				}
			}
		}
		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (String s : list) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m != null) {
					replaceWith.add(m);
				} else {
					MagicSpells.error("ReplaceSpell " + spellName + " invalid replace-with item: " + s);
				}
			}
		}
		
		if (!replaceRandom && replace.size() != replaceWith.size()) {
			replaceRandom = true;
			MagicSpells.error("ReplaceSpell " + spellName + " replace-random false, but replace-blocks and replace-with have different sizes!");
		}
		
		if (replace.size() == 0) {
			MagicSpells.error("ReplaceSpell " + spellName + " has empty replace-blocks list!");
		}
		if (replaceWith.size() == 0) {
			MagicSpells.error("ReplaceSpell " + spellName + " has empty replace-with list!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pointBlank ? player.getLocation().getBlock() : getTargetedBlock(player, power);
			if (target == null) {
				return noTarget(player);
			}
			castAtLocation(player, target.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean replaced = false;
		Block block;
		int d = powerAffectsRadius ? Math.round(radiusDown * power) : radiusDown;
		int u = powerAffectsRadius ? Math.round(radiusUp * power) : radiusUp;
		int h = powerAffectsRadius ? Math.round(radiusHoriz * power) : radiusHoriz;
		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						if (replace.get(i).equals(block)) {
							if (replaceRandom) {
								replaceWith.get(random.nextInt(replaceWith.size())).setBlock(block, true);
							} else {
								replaceWith.get(i).setBlock(block, true);
							}
							replaced = true;
							break;
						}
					}
				}			
			}
		}
		if (caster != null) {
			playSpellEffects(caster, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		return replaced;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

}
