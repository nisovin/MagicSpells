package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LightwalkSpell extends BuffSpell {
	
	private HashMap<String,Block> lightwalkers;

	public LightwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
				
		lightwalkers = new HashMap<String,Block>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (lightwalkers.containsKey(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			lightwalkers.put(player.getName(), null);
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (lightwalkers.containsKey(event.getPlayer().getName())) {
			Player p = event.getPlayer();
			Block oldBlock = lightwalkers.get(p.getName());
			Block newBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
			if ((oldBlock == null || !oldBlock.equals(newBlock)) && allowedType(newBlock.getType()) && newBlock.getType() != Material.AIR) {
				if (isExpired(p)) {
					turnOff(p);
				} else {
					if (oldBlock != null) {
						p.sendBlockChange(oldBlock.getLocation(), oldBlock.getType(), oldBlock.getData());
					}
					p.sendBlockChange(newBlock.getLocation(), Material.GLOWSTONE, (byte)0);
					lightwalkers.put(p.getName(), newBlock);
					addUse(p);
					chargeUseCost(p);
				}
			}
		}
	}
	
	private boolean allowedType(Material mat) {
		return mat == Material.DIRT || 
			mat == Material.GRASS ||
			mat == Material.GRAVEL ||
			mat == Material.STONE ||
			mat == Material.COBBLESTONE ||
			mat == Material.WOOD || 
			mat == Material.LOG || 
			mat == Material.NETHERRACK ||
			mat == Material.SOUL_SAND ||
			mat == Material.SAND ||
			mat == Material.SANDSTONE ||
			mat == Material.GLASS ||
			mat == Material.WOOL ||
			mat == Material.DOUBLE_STEP ||
			mat == Material.BRICK ||
			mat == Material.OBSIDIAN;
	}
	
	@Override
	public void turnOff(Player player) {
		Block b = lightwalkers.get(player.getName());
		if (b != null) {
			super.turnOff(player);
			player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
			lightwalkers.remove(player.getName());
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		for (String s : lightwalkers.keySet()) {
			Player p = Bukkit.getServer().getPlayer(s);
			if (p != null) {
				Block b = lightwalkers.get(s);
				p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
			}
		}
		lightwalkers.clear();
	}

}
