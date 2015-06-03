package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class SteedSpell extends InstantSpell {

	Random random = new Random();
	
	Set<String> mounted = new HashSet<String>();
	
	EntityType type;
	Horse.Color color = null;
	Horse.Style style = null;
	Horse.Variant variant = null;
	
	ItemStack armor;
	
	String strAlreadyMounted;
	
	public SteedSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = Util.getEntityType(getConfigString("type", "horse"));
		if (type == EntityType.HORSE) {
			String c = getConfigString("color", null);
			String s = getConfigString("style", null);
			String v = getConfigString("variant", null);
			String a = getConfigString("armor", null);
			if (c != null) {
				for (Horse.Color h : Horse.Color.values()) {
					if (h.name().equalsIgnoreCase(c)) {
						color = h;
						break;
					}
				}
			}
			if (s != null) {
				for (Horse.Style h : Horse.Style.values()) {
					if (h.name().equalsIgnoreCase(s)) {
						style = h;
						break;
					}
				}
			}
			if (v != null) {
				for (Horse.Variant h : Horse.Variant.values()) {
					if (h.name().equalsIgnoreCase(v)) {
						variant = h;
						break;
					}
				}
			}
			if (a != null) {
				armor = Util.getItemStackFromString(a);
			}
		}
		
		strAlreadyMounted = getConfigString("str-already-mounted", "You are already mounted!");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (player.getVehicle() != null) {
				sendMessage(player, strAlreadyMounted);
				return PostCastAction.ALREADY_HANDLED;
			}
			Entity entity = player.getWorld().spawnEntity(player.getLocation(), type);
			if (type == EntityType.HORSE) {
				((Horse)entity).setTamed(true);
				((Horse)entity).setOwner(player);
				((Horse)entity).setJumpStrength(2d);
				if (color != null) {
					((Horse)entity).setColor(color);
				} else {
					((Horse)entity).setColor(Horse.Color.values()[random.nextInt(Horse.Color.values().length)]);
				}
				if (style != null) {
					((Horse)entity).setStyle(style);
				} else {
					((Horse)entity).setStyle(Horse.Style.values()[random.nextInt(Horse.Style.values().length)]);
				}
				if (variant != null) {
					((Horse)entity).setVariant(variant);
				} else {
					((Horse)entity).setVariant(Horse.Variant.values()[random.nextInt(Horse.Variant.values().length)]);
				}
				((Horse)entity).setTamed(true);
				((Horse)entity).setOwner(player);
				((Horse)entity).getInventory().setSaddle(new ItemStack(Material.SADDLE));
				if (armor != null) {
					((Horse)entity).getInventory().setArmor(armor);
				}
			}
			entity.setPassenger(player);
			playSpellEffects(EffectPosition.CASTER, player);
			mounted.add(player.getName());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onDismount(EntityDismountEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			if (mounted.contains(player.getName())) {
				mounted.remove(player.getName());
				event.getDismounted().remove();
				playSpellEffects(EffectPosition.DISABLED, player);
			}
		}
	}
	
	@EventHandler
	void onQuit(PlayerQuitEvent event) {
		if (mounted.contains(event.getPlayer().getName()) && event.getPlayer().getVehicle() != null) {
			mounted.remove(event.getPlayer().getName());
			Entity vehicle = event.getPlayer().getVehicle();
			vehicle.eject();
			vehicle.remove();
		}
	}
	
	@Override
	public void turnOff() {
		for (String name : mounted) {
			@SuppressWarnings("deprecation")
			Player player = Bukkit.getPlayerExact(name);
			if (player != null && player.getVehicle() != null) {
				player.getVehicle().eject();
			}
		}
		mounted.clear();
	}

}
