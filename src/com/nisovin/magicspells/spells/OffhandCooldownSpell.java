package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class OffhandCooldownSpell extends InstantSpell {

	String spellToCheck;
	Spell spell;
	ItemStack item;
	
	List<Player> players = new ArrayList<Player>();
	
	public OffhandCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellToCheck = getConfigString("spell", "");
		if (isConfigString("item")) {
			item = Util.getItemStackFromString(getConfigString("item", "stone"));
		} else if (isConfigSection("item")) {
			item = Util.getItemStackFromConfig(getConfigSection("item"));
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		spell = MagicSpells.getSpellByInternalName(spellToCheck);
		
		if (spell != null && item != null) {
			MagicSpells.scheduleRepeatingTask(new Runnable() {
				public void run() {
					Iterator<Player> iter = players.iterator();
					while (iter.hasNext()) {
						Player p = iter.next();
						if (!p.isValid()) {
							iter.remove();
						} else {
							float cd = spell.getCooldown(p);
							int amt = 1;
							if (cd > 0) {
								amt = -(int)Math.ceil(cd);
							}
							ItemStack off = p.getInventory().getItemInOffHand();
							if (off == null || !off.isSimilar(item)) {
								p.getInventory().setItemInOffHand(item.clone());
							}
							p.getInventory().getItemInOffHand().setAmount(amt);
						}
					}
				}
			}, 20, 20);
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			players.add(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
