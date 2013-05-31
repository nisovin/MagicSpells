package com.nisovin.magicspells.spells.instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class EnderchestSpell extends InstantSpell {

	public EnderchestSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args != null && args.length == 1 && player.hasPermission("magicspells.advanced." + internalName)) {
				Player target = Bukkit.getPlayer(args[0]);
				if (target == null) {
					player.sendMessage("Invalid player target");
					return PostCastAction.ALREADY_HANDLED;
				}
				player.openInventory(target.getEnderChest());
			} else {
				player.openInventory(player.getEnderChest());
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
