package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class RemoveMarksSpell extends TargetedSpell implements TargetedLocationSpell {

	float radius;
	boolean pointBlank;
	String markSpellName;
	MarkSpell markSpell;
	
	public RemoveMarksSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		radius = getConfigFloat("radius", 10);
		pointBlank = getConfigBoolean("point-blank", false);
		markSpellName = getConfigString("mark-spell", "mark");
	}

	@Override
	public void initialize() {
		super.initialize();
		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell != null && spell instanceof MarkSpell) {
			markSpell = (MarkSpell)spell;
		} else {
			MagicSpells.error("Failed to get mark spell for '" + internalName + "' spell");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) {
				loc = player.getLocation();
			} else {
				Block b = getTargetedBlock(player, power);
				if (b != null && b.getType() != Material.AIR) {
					loc = b.getLocation();
				}
			}
			if (loc == null) {
				return noTarget(player);
			}
			removeMarks(player, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void removeMarks(Player caster, Location loc, float power) {
		float rad = radius * power;
		float radSq = rad * rad;
		HashMap<String, MagicLocation> marks = markSpell.getMarks();
		Iterator<String> iter = marks.keySet().iterator();
		while (iter.hasNext()) {
			MagicLocation l = marks.get(iter.next());
			if (l.getWorld().equals(loc.getWorld().getName())) {
				if (l.getLocation().distanceSquared(loc) < radSq) {
					iter.remove();
				}
			}
		}
		markSpell.setMarks(marks);
		if (caster != null) {
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		playSpellEffects(EffectPosition.TARGET, loc);
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		removeMarks(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		removeMarks(null, target, power);
		return true;
	}
}
