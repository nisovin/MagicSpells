package com.nisovin.magicspells.castmodifiers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.*;

public class ModifierSet {

	public static void initializeModifierListeners() {
		boolean modifiers = false;
		boolean targetModifiers = false;		
		for (Spell spell : MagicSpells.spells()) {
			if (spell.getModifiers() != null) {
				modifiers = true;
			}
			if (spell.getTargetModifiers() != null) {
				targetModifiers = true;
			}
			if (modifiers && targetModifiers) break;
		}
		
		if (modifiers) {
			MagicSpells.registerEvents(new CastListener());
		}
		if (targetModifiers) {
			MagicSpells.registerEvents(new TargetListener());
		}
		if (MagicSpells.getManaHandler() != null && MagicSpells.getManaHandler().getModifiers() != null) {
			MagicSpells.registerEvents(new ManaListener());
		}
	}

	private List<Modifier> modifiers;
	
	public ModifierSet(List<String> data) {
		modifiers = new ArrayList<Modifier>();
		for (String s : data) {
			Modifier m = Modifier.factory(s);
			if (m != null) {
				modifiers.add(m);
				MagicSpells.debug(3, "    Modifier added: " + s);
			} else {
				MagicSpells.error("Problem with modifier: " + s);
			}
		}
	}
	
	public void apply(SpellCastEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				String msg = modifier.strModifierFailed != null ? modifier.strModifierFailed : event.getSpell().getStrModifierFailed();
				MagicSpells.sendMessage(event.getCaster(), msg);
				break;
			}
		}
	}
	
	public void apply(ManaChangeEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				break;
			}
		}
	}
	
	public void apply(SpellTargetEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				if (modifier.strModifierFailed != null) {
					MagicSpells.sendMessage(event.getCaster(), modifier.strModifierFailed);
				}
				break;
			}
		}
	}
	
	public void apply(MagicSpellsGenericPlayerEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				break;
			}
		}
	}
	
	public void apply(SpellTargetLocationEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				break;
			}
		}
	}
	
	public boolean check(Player player) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(player);
			if (!pass) return false;
		}
		return true;
	}
	
}
