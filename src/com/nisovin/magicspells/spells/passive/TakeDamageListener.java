package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicItemWithNameMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;

public class TakeDamageListener extends PassiveListener {

	Map<DamageCause, List<PassiveSpell>> damageCauses = new HashMap<EntityDamageEvent.DamageCause, List<PassiveSpell>>();
	Set<Material> types = new HashSet<Material>();
	Map<MagicMaterial, List<PassiveSpell>> weapons = new LinkedHashMap<MagicMaterial, List<PassiveSpell>>();
	List<PassiveSpell> always = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			always.add(spell);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				s = s.trim();
				boolean isDamCause = false;
				for (DamageCause c : DamageCause.values()) {
					if (s.equalsIgnoreCase(c.name())) {
						List<PassiveSpell> spells = damageCauses.get(c);
						if (spells == null) {
							spells = new ArrayList<PassiveSpell>();
							damageCauses.put(c, spells);
						}
						spells.add(spell);
						isDamCause = true;
						break;
					}
				}
				if (!isDamCause) {
					MagicMaterial mat = null;
					if (s.contains("|")) {
						String[] stuff = s.split("\\|");
						mat = MagicSpells.getItemNameResolver().resolveItem(stuff[0]);
						if (mat != null) {
							mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
						}
					} else {
						mat = MagicSpells.getItemNameResolver().resolveItem(s);
					}
					if (mat != null) {
						List<PassiveSpell> list = weapons.get(mat);
						if (list == null) {
							list = new ArrayList<PassiveSpell>();
							weapons.put(mat, list);
						}
						list.add(spell);
						types.add(mat.getMaterial());
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player)event.getEntity();
		LivingEntity attacker = null;
		Spellbook spellbook = null;
		
		if (!always.isEmpty()) {
			spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : always) {
				if (spellbook.hasSpell(spell, false)) {
					boolean casted = spell.activate(player, attacker);
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		
		if (!damageCauses.isEmpty()) {
			List<PassiveSpell> causeSpells = damageCauses.get(event.getCause());
			if (causeSpells != null && causeSpells.size() > 0) {
				attacker = getAttacker(event);
				if (spellbook == null) spellbook = MagicSpells.getSpellbook(player);
				for (PassiveSpell spell : causeSpells) {
					if (spellbook.hasSpell(spell, false)) {
						boolean casted = spell.activate(player, attacker);
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
		
		if (!weapons.isEmpty()) {
			if (attacker == null) attacker = getAttacker(event);
			if (attacker != null && attacker instanceof Player) {
				Player playerAttacker = (Player)attacker;
				ItemStack item = playerAttacker.getItemInHand();
				if (item != null && item.getType() != Material.AIR) {
					List<PassiveSpell> list = getSpells(item);
					if (list != null) {
						if (spellbook == null) spellbook = MagicSpells.getSpellbook(player);
						for (PassiveSpell spell : list) {
							if (spellbook.hasSpell(spell, false)) {
								boolean casted = spell.activate(player, attacker);
								if (casted && spell.cancelDefaultAction()) {
									event.setCancelled(true);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private LivingEntity getAttacker(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) {
			Entity e = ((EntityDamageByEntityEvent)event).getDamager();
			if (e instanceof LivingEntity) {
				return (LivingEntity)e;
			} else if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof LivingEntity) {
				return (LivingEntity)((Projectile)e).getShooter();
			}
		}
		return null;
	}
	
	private List<PassiveSpell> getSpells(ItemStack item) {
		if (types.contains(item.getType())) {
			for (MagicMaterial m : weapons.keySet()) {
				if (m.equals(item)) {
					return weapons.get(m);
				}
			}
		}
		return null;
	}

}
