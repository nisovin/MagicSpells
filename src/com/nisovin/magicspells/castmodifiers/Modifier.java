package com.nisovin.magicspells.castmodifiers;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.Util;

public class Modifier {

	boolean negated = false;
	Condition condition;
	ModifierType type;
	String modifierVar;
	float modifierVarFloat;
	int modifierVarInt;
	String modifierVarString;
	String strModifierFailed = null;
	
	public static Modifier factory(String s) {
		Modifier m = new Modifier();
		String[] s1 = s.split("\\$\\$");
		String[] data = Util.splitParams(s1[0].trim());
		if (data.length < 2) return null;
				
		// get condition
		if (data[0].startsWith("!")) {
			m.negated = true;
			data[0] = data[0].substring(1);
		}
		m.condition = Condition.getConditionByName(data[0]);
		if (m.condition == null) return null;
		
		// get type and vars
		m.type = getTypeByName(data[1]);
		if (m.type == null && data.length > 2) {
			boolean varok = m.condition.setVar(data[1]);
			if (!varok) return null;
			m.type = getTypeByName(data[2]);
			if (data.length > 3) {
				m.modifierVar = data[3];
			}
		} else if (data.length > 2) {
			m.modifierVar = data[2];
		}
		
		// check type
		if (m.type == null) return null;
		
		// process modifiervar
		try {
			if (m.type == ModifierType.POWER || m.type == ModifierType.ADD_POWER || m.type == ModifierType.COOLDOWN || m.type == ModifierType.REAGENTS) {
				m.modifierVarFloat = Float.parseFloat(m.modifierVar);
			} else if (m.type == ModifierType.CAST_TIME) {
				m.modifierVarInt = Integer.parseInt(m.modifierVar);
			}
		} catch (NumberFormatException e) {
			return null;
		}
		
		// check for failed string
		if (s1.length > 1) {
			m.strModifierFailed = s1[1].trim();
		}
		
		// done
		return m;
	}
	
	public boolean apply(SpellCastEvent event) {
		Player player = event.getCaster();
		boolean check = condition.check(player);
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.CAST) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getCaster(), event.getPower(), event.getSpellArgs());
			}
		} else if (check == true && type == ModifierType.CAST_INSTEAD) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getCaster(), event.getPower(), event.getSpellArgs());
			}
			event.setCancelled(true);
		} else if (check == true) {
			if (type == ModifierType.STOP) {
				return false;
			} else if (type == ModifierType.POWER) {
				event.increasePower(modifierVarFloat);
			} else if (type == ModifierType.ADD_POWER) {
				event.setPower(event.getPower() + modifierVarFloat);
			} else if (type == ModifierType.COOLDOWN) {
				event.setCooldown(modifierVarFloat);
			} else if (type == ModifierType.REAGENTS) {
				event.setReagents(event.getReagents().multiply(modifierVarFloat));
			} else if (type == ModifierType.CAST_TIME) {
				event.setCastTime(modifierVarInt);
			}
		} else if (check == false && type == ModifierType.CONTINUE) {
			return false;
		}
		return true;
	}
	
	public boolean apply(ManaChangeEvent event) {
		Player player = event.getPlayer();
		boolean check = condition.check(player);
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			event.setNewAmount(event.getOldAmount());
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			event.setNewAmount(event.getOldAmount());
			return false;
		} else if (check == true && type == ModifierType.STOP) {
			return false;
		} else if (check == false && type == ModifierType.CONTINUE) {
			return false;
		} else if (check == true && type == ModifierType.POWER) {
			int gain = event.getNewAmount() - event.getOldAmount();
			gain = Math.round(gain * modifierVarFloat);
			int newAmt = event.getOldAmount() + gain;
			if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
			event.setNewAmount(newAmt);
		} else if (check == true && type == ModifierType.ADD_POWER) {
			int newAmt = event.getNewAmount() + (int)modifierVarFloat;
			if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
			if (newAmt < 0) newAmt = 0;
			event.setNewAmount(newAmt);
		} else if (check == true && (type == ModifierType.CAST || type == ModifierType.CAST_INSTEAD)) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getPlayer(), 1, null);
			}
		}
		return true;
	}
	
	public boolean apply(SpellTargetEvent event) {
		Player player = event.getCaster();
		boolean check = condition.check(player, event.getTarget());
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.STOP) {
			return false;
		} else if (check == false && type == ModifierType.CONTINUE) {
			return false;
		} else if (check == true && type == ModifierType.POWER) {
			event.increasePower(modifierVarFloat);
		} else if (check == true && type == ModifierType.ADD_POWER) {
			event.setPower(event.getPower() + modifierVarFloat);
		} else if (check == true && type == ModifierType.CAST) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getCaster(), 1, null);
			}
		} else if (check == true && type == ModifierType.CAST_INSTEAD) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				if (spell instanceof TargetedEntitySpell) {
					((TargetedEntitySpell)spell).castAtEntity(event.getCaster(), event.getTarget(), 1F);
				} else {
					spell.castSpell(event.getCaster(), SpellCastState.NORMAL, 1F, null);
				}
			}
		}
		return true;
	}
	
	public boolean apply(SpellTargetLocationEvent event) {
		Player player = event.getCaster();
		boolean check = condition.check(player, event.getTargetLocation());
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.STOP) {
			return false;
		} else if (check == false && type == ModifierType.CONTINUE) {
			return false;
		} else if (check == true && (type == ModifierType.CAST || type == ModifierType.CAST_INSTEAD)) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null && spell instanceof TargetedLocationSpell) {
				((TargetedLocationSpell)spell).castAtLocation(event.getCaster(), event.getTargetLocation(), 1F);
				if (type == ModifierType.CAST_INSTEAD) {
					event.setCancelled(true);
				}
			}
		}
		return true;
	}
	
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		boolean check = condition.check(event.getPlayer());
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			event.setCancelled(true);
			return false;
		} else if (check == true && type == ModifierType.STOP) {
			return false;
		} else if (check == false && type == ModifierType.CONTINUE) {
			return false;
		} else if (check == true && type == ModifierType.CAST) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getPlayer(), 1, null);
			}
		} else if (check == true && type == ModifierType.CAST_INSTEAD) {
			Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
			if (spell != null) {
				spell.cast(event.getPlayer(), 1, null);
			}
		}
		return true;
	}
	
	public boolean check(Player player) {
		boolean check = condition.check(player);
		if (negated) check = !check;
		if (check == false && type == ModifierType.REQUIRED) {
			return false;
		} else if (check == true && type == ModifierType.DENIED) {
			return false;
		}
		return true;
	}
	
	private static ModifierType getTypeByName(String name) {
		if (name.equalsIgnoreCase("required") || name.equalsIgnoreCase("require")) {
			return ModifierType.REQUIRED;
		} else if (name.equalsIgnoreCase("denied") || name.equalsIgnoreCase("deny")) {
			return ModifierType.DENIED;
		} else if (name.equalsIgnoreCase("power") || name.equalsIgnoreCase("empower") || name.equalsIgnoreCase("multiply")) {
			return ModifierType.POWER;
		} else if (name.equalsIgnoreCase("addpower") || name.equalsIgnoreCase("add")) {
			return ModifierType.ADD_POWER;
		} else if (name.equalsIgnoreCase("cooldown")) {
			return ModifierType.COOLDOWN;
		} else if (name.equalsIgnoreCase("reagents")) {
			return ModifierType.REAGENTS;
		} else if (name.equalsIgnoreCase("casttime")) {
			return ModifierType.CAST_TIME;
		} else if (name.equalsIgnoreCase("stop")) {
			return ModifierType.STOP;
		} else if (name.equalsIgnoreCase("continue")) {
			return ModifierType.CONTINUE;
		} else if (name.equalsIgnoreCase("cast")) {
			return ModifierType.CAST;
		} else if (name.equalsIgnoreCase("castinstead")) {
			return ModifierType.CAST_INSTEAD;
		} else {
			return null;
		}
	}
	
	private enum ModifierType {
		REQUIRED,
		DENIED,
		POWER,
		ADD_POWER,
		COOLDOWN,
		REAGENTS,
		CAST_TIME,
		STOP,
		CONTINUE,
		CAST,
		CAST_INSTEAD
	}
	
}
