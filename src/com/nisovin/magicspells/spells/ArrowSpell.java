package com.nisovin.magicspells.spells;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
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
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;

public class ArrowSpell extends Spell {

	private static ArrowSpellHandler handler;
	
	String bowName;
	String spellNameOnHitEntity;
	String spellNameOnHitGround;
	Subspell spellOnHitEntity;
	Subspell spellOnHitGround;
	boolean useBowForce;
	
	public ArrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", null));
		spellNameOnHitEntity = getConfigString("spell-on-hit-entity", null);
		spellNameOnHitGround = getConfigString("spell-on-hit-ground", null);
		useBowForce = getConfigBoolean("use-bow-force", true);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (spellNameOnHitEntity != null && !spellNameOnHitEntity.isEmpty()) {
			Subspell spell = new Subspell(spellNameOnHitEntity);
			if (spell.process() && spell.isTargetedEntitySpell()) {
				spellOnHitEntity = spell;
			}
		}
		if (spellNameOnHitGround != null && !spellNameOnHitGround.isEmpty()) {
			Subspell spell = new Subspell(spellNameOnHitGround);
			if (spell.process() && spell.isTargetedLocationSpell()) {
				spellOnHitGround = spell;
			}
		}
		
		if (handler == null) {
			handler = new ArrowSpellHandler();
		}
		handler.registerSpell(this);
	}
	
	@Override
	public void turnOff() {
		super.turnOff();
		if (handler != null) {
			handler.turnOff();
			handler = null;
		}
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
					SpellReagents reagents = spell.reagents.clone();
					SpellCastEvent castEvent = new SpellCastEvent(spell, shooter, SpellCastState.NORMAL, useBowForce ? event.getForce() : 1.0F, null, cooldown, reagents, castTime);
					Bukkit.getPluginManager().callEvent(castEvent);
					if (!castEvent.isCancelled()) {
						event.getProjectile().setMetadata("MSArrowSpell", new FixedMetadataValue(MagicSpells.plugin, new ArrowSpellData(spell, castEvent.getPower(), castEvent.getReagents())));
					} else {
						event.setCancelled(true);
						event.getProjectile().remove();
					}
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
							if (!data.casted && !data.spell.onCooldown(shooter) && data.spell.hasReagents(shooter, data.reagents)) {
								boolean success = data.spell.spellOnHitGround.castAtLocation(shooter, arrow.getLocation(), data.power);
								if (success) {
									data.spell.setCooldown(shooter, data.spell.cooldown);
									data.spell.removeReagents(shooter, data.reagents);
								}
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

		@EventHandler(ignoreCancelled=true)
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
						SpellTargetEvent evt = new SpellTargetEvent(data.spell, shooter, (LivingEntity)event.getEntity(), data.power);
						Bukkit.getPluginManager().callEvent(evt);
						if (!evt.isCancelled()) {
							data.spell.spellOnHitEntity.castAtEntity(shooter, (LivingEntity)event.getEntity(), evt.getPower());
							data.spell.setCooldown(shooter, data.spell.cooldown);
						}
						data.casted = true;
					}
				}
				break;
			}
			arrow.remove();
			arrow.removeMetadata("MSArrowSpell", MagicSpells.plugin);
		}
		
		public void turnOff() {
			unregisterEvents(this);
			spells.clear();
		}
		
	}
	
	class ArrowSpellData {
		ArrowSpell spell;
		boolean casted = false;
		float power = 1.0F;
		SpellReagents reagents;
		public ArrowSpellData(ArrowSpell spell, float power, SpellReagents reagents) {
			this.spell = spell;
			this.power = power;
			this.reagents = reagents;
		}
	}

}
