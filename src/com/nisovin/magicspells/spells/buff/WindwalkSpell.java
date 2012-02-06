package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WindwalkSpell extends BuffSpell {

	private boolean cancelOnLand;
	
	private HashMap<Player, SavedInventory> flyers;
	private HashMap<Player, Integer> tasks;
	
	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		cancelOnLand = getConfigBoolean("cancel-on-land", true);
				
		flyers = new HashMap<Player, SavedInventory>();
		if (useCostInterval > 0) {
			tasks = new HashMap<Player, Integer>();
		}
	}

	@Override
	public PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
		if (flyers.containsKey(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			// set flying
			SavedInventory inv = new SavedInventory(player);
			flyers.put(player, inv);
			player.getInventory().clear();
			player.setGameMode(GameMode.CREATIVE);
			// set duration limit
			if (duration > 0) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						turnOff(player);
					}
				}, duration*20);
			}
			// set cost interval
			if (useCostInterval > 0) {
				int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						addUseAndChargeCost(player);
					}
				}, useCostInterval*20, useCostInterval*20);
				tasks.put(player, taskId);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if (cancelOnLand && flyers.containsKey(event.getPlayer())) {
			if (event.getPlayer().getLocation().subtract(0,1,0).getBlock().getType() != Material.AIR) {
				turnOff(event.getPlayer());
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled() && flyers.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.isCancelled() && flyers.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (!event.isCancelled() && flyers.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.isCancelled() && event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
			if (flyers.containsKey(e.getDamager())) {
				event.setCancelled(true);
			} else if (e.getDamager() instanceof Projectile && flyers.containsKey(((Projectile)e.getDamager()).getShooter())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled() || !event.hasBlock()) return;
		if (flyers.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (flyers.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@Override
	public void turnOff(final Player player) {
		super.turnOff(player);
		if (flyers.containsKey(player)) {
			player.setGameMode(GameMode.SURVIVAL);
			flyers.remove(player).restore(player);
			sendMessage(player, strFade);
		}
		if (tasks != null && tasks.containsKey(player)) {
			int taskId = tasks.remove(player);
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}
	
	@Override
	protected void turnOff() {
		HashMap<Player,SavedInventory> flyers = new HashMap<Player,SavedInventory>(this.flyers);
		for (Player player : new HashSet<Player>(flyers.keySet())) {
			turnOff(player);
		}
		this.flyers.clear();
	}
	
	private class SavedInventory {
		private ItemStack[] contents;
		private ItemStack head;
		private ItemStack chest;
		private ItemStack legs;
		private ItemStack feet;
		
		public SavedInventory(Player player) {
			PlayerInventory inv = player.getInventory();
			ItemStack[] invContents = inv.getContents();
			contents = new ItemStack[invContents.length];
			for (int i = 0; i < contents.length; i++) {
				if (invContents[i] == null) {
					contents[i] = null;
				} else {
					contents[i] = invContents[i].clone();
				}
			}
			ItemStack temp;
			temp = inv.getHelmet();
			if (temp != null && temp.getType() != Material.AIR) head = temp.clone();
			temp = inv.getChestplate();
			if (temp != null && temp.getType() != Material.AIR) chest = temp.clone();
			temp = inv.getLeggings();
			if (temp != null && temp.getType() != Material.AIR) legs = temp.clone();
			temp = inv.getBoots();
			if (temp != null && temp.getType() != Material.AIR) feet = temp.clone();
		}
		
		@SuppressWarnings("deprecation")
		public void restore(Player player) {
			PlayerInventory inv = player.getInventory();
			inv.setContents(contents);
			inv.setHelmet(head);
			inv.setChestplate(chest);
			inv.setLeggings(legs);
			inv.setBoots(feet);
			player.updateInventory();
		}
	}

}
