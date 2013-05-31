package com.nisovin.magicspells.spells;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.MagicConfig;

public class ArrowSpell extends Spell {

	private static ArrowSpellHandler handler;
	
	String bowName;
	String spellNameOnHitEntity;
	String spellNameOnHitGround;
	TargetedEntitySpell spellOnHitEntity;
	TargetedLocationSpell spellOnHitGround;
	
	public ArrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", null));
		spellNameOnHitEntity = getConfigString("spell-on-hit-entity", null);
		spellNameOnHitGround = getConfigString("spell-on-hit-ground", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (spellNameOnHitEntity != null && !spellNameOnHitEntity.isEmpty()) {
			Spell spell = MagicSpells.getSpellByInternalName(spellNameOnHitEntity);
			if (spell != null && spell instanceof TargetedEntitySpell) {
				spellOnHitEntity = (TargetedEntitySpell)spell;
			}
		}
		if (spellNameOnHitGround != null && !spellNameOnHitGround.isEmpty()) {
			Spell spell = MagicSpells.getSpellByInternalName(spellNameOnHitGround);
			if (spell != null && spell instanceof TargetedLocationSpell) {
				spellOnHitGround = (TargetedLocationSpell)spell;
			}
		}
		
		if (handler == null) {
			handler = new ArrowSpellHandler();
		}
		handler.registerSpell(this);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}
	
	class ArrowSpellHandler implements Listener {
		
		Map<String, ArrowSpell> spells = new HashMap<String, ArrowSpell>();
		
		public ArrowSpellHandler() {
			registerEvents(this);
		}
		
		public void registerSpell(ArrowSpell spell) {
			spells.put(spell.bowName, spell);
		}
		
		@EventHandler
		public void onArrowLaunch(EntityShootBowEvent event) {
			if (event.getEntity().getType() != EntityType.PLAYER) return;
			Player shooter = (Player)event.getEntity();
			ItemStack inHand = shooter.getItemInHand();
			if (inHand == null || inHand.getType() != Material.BOW) return;
			String bowName = inHand.getItemMeta().getDisplayName();
			if (bowName != null && !bowName.isEmpty()) {
				Spellbook spellbook = MagicSpells.getSpellbook(shooter);
				ArrowSpell spell = spells.get(bowName);
				if (spell != null && spellbook.hasSpell(spell) && spellbook.canCast(spell)) {
					event.getProjectile().setMetadata("MSArrowSpell", new FixedMetadataValue(MagicSpells.plugin, new ArrowSpellData(spell)));
				}
			}
		}

		@EventHandler
		public void onArrowHit(ProjectileHitEvent event) {
			final Projectile arrow = event.getEntity();
			if (arrow.getType() != EntityType.ARROW) return;
			List<MetadataValue> metas = arrow.getMetadata("MSArrowSpell");
			if (metas == null || metas.size() == 0) return;
			for (MetadataValue meta : metas) {
				final ArrowSpellData data = (ArrowSpellData)meta.value();
				if (data.spell.spellOnHitGround != null) {
					MagicSpells.scheduleDelayedTask(new Runnable() {
						public void run() {
							Player shooter = (Player)arrow.getShooter();
							if (!data.casted && !data.spell.onCooldown(shooter)) {
								data.spell.spellOnHitGround.castAtLocation(shooter, arrow.getLocation(), 1.0F);
								data.spell.setCooldown(shooter, data.spell.cooldown);
								data.casted = true;
								arrow.removeMetadata("MSArrowSpell", MagicSpells.plugin);
							}
						}
					}, 0);
				}
				break;
			}
			arrow.remove();
		}

		@EventHandler
		public void onArrowHitEntity(EntityDamageByEntityEvent event) {
			if (event.getDamager().getType() != EntityType.ARROW) return;
			if (!(event.getEntity() instanceof LivingEntity)) return;
			Projectile arrow = (Projectile)event.getDamager();
			List<MetadataValue> metas = arrow.getMetadata("MSArrowSpell");
			if (metas == null || metas.size() == 0) return;
			Player shooter = (Player)arrow.getShooter();
			for (MetadataValue meta : metas) {
				ArrowSpellData data = (ArrowSpellData)meta.value();
				if (!data.spell.onCooldown(shooter)) {
					if (data.spell.spellOnHitEntity != null) {
						data.spell.spellOnHitEntity.castAtEntity(shooter, (LivingEntity)event.getEntity(), 1.0F);
						data.spell.setCooldown(shooter, data.spell.cooldown);
						data.casted = true;
					} else if (data.spell.spellOnHitGround != null) {
						data.spell.spellOnHitGround.castAtLocation(shooter, arrow.getLocation(), 1.0F);
						data.spell.setCooldown(shooter, data.spell.cooldown);
						data.casted = true;
					}
				}
				break;
			}
			arrow.remove();
			arrow.removeMetadata("MSArrowSpell", MagicSpells.plugin);
		}
		
	}
	
	class ArrowSpellData {
		ArrowSpell spell;
		boolean casted = false;
		public ArrowSpellData(ArrowSpell spell) {
			this.spell = spell;
		}
	}

}
