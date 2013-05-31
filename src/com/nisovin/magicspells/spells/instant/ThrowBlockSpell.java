package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.ItemNameResolver.ItemTypeAndData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ThrowBlockSpell extends InstantSpell {

	int blockType;
	byte blockData;
	float velocity;
	boolean applySpellPowerToVelocity;
	float verticalAdjustment;
	int rotationOffset;
	float fallDamage;
	int fallDamageMax;
	boolean dropItem;
	boolean removeBlocks;
	boolean preventBlocks;
	boolean callTargetEvent;
	boolean checkPlugins;
	String spellOnLand;
	TargetedLocationSpell spell;
	
	Map<FallingBlock, FallingBlockInfo> fallingBlocks;
	int cleanTask = -1;
	
	public ThrowBlockSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String blockTypeInfo = getConfigString("block-type", Material.ANVIL.getId() + "");
		ItemTypeAndData typeAndData = MagicSpells.getItemNameResolver().resolve(blockTypeInfo);
		blockType = typeAndData.id;
		blockData = (byte)typeAndData.data;
		velocity = getConfigFloat("velocity", 1);
		applySpellPowerToVelocity = getConfigBoolean("apply-spell-power-to-velocity", false);
		verticalAdjustment = getConfigFloat("vertical-adjustment", 0.5F);
		rotationOffset = getConfigInt("rotation-offset", 0);
		fallDamage = getConfigFloat("fall-damage", 2.0F);
		fallDamageMax = getConfigInt("fall-damage-max", 20);
		dropItem = getConfigBoolean("drop-item", false);
		removeBlocks = getConfigBoolean("remove-blocks", false);
		preventBlocks = getConfigBoolean("prevent-blocks", false);
		callTargetEvent = getConfigBoolean("call-target-event", true);
		checkPlugins = getConfigBoolean("check-plugins", false);
		spellOnLand = getConfigString("spell-on-land", null);
	}	
	
	@Override
	public void initialize() {
		super.initialize();
		if (spellOnLand != null && !spellOnLand.isEmpty()) {
			Spell s = MagicSpells.getSpellByInternalName(spellOnLand);
			if (s != null && s instanceof TargetedLocationSpell) {
				spell = (TargetedLocationSpell)s;
			} else {
				MagicSpells.error("Invalid spell-on-land for " + name + " spell");
			}
		}
		if (fallDamage > 0 || removeBlocks || preventBlocks || spell != null) {
			fallingBlocks = new HashMap<FallingBlock, ThrowBlockSpell.FallingBlockInfo>();
			registerEvents(new ThrowBlockListener(this));
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = player.getLocation().getDirection();
			if (verticalAdjustment != 0) {
				v.setY(v.getY() + verticalAdjustment);
			}
			if (rotationOffset != 0) {
				Util.rotateVector(v, rotationOffset);
			}
			v.normalize().multiply(velocity);
			if (applySpellPowerToVelocity) {
				v.multiply(power);
			}
			FallingBlock block = player.getWorld().spawnFallingBlock(player.getEyeLocation().add(v), blockType, blockData);
			block.setVelocity(v);
			block.setDropItem(dropItem);
			if (fallDamage > 0) {
				MagicSpells.getVolatileCodeHandler().setFallingBlockHurtEntities(block, fallDamage, fallDamageMax);
			}
			if (fallingBlocks != null) {
				fallingBlocks.put(block, new FallingBlockInfo(player, power));
				if (cleanTask < 0) {
					startTask();
				}
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void startTask() {
		cleanTask = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				Iterator<FallingBlock> iter = fallingBlocks.keySet().iterator();
				while (iter.hasNext()) {
					FallingBlock block = iter.next();
					if (!block.isValid()) {
						iter.remove();
						if (removeBlocks) {
							Block b = block.getLocation().getBlock();
							if (b.getTypeId() == blockType && (b.getData() == blockData || blockType == Material.ANVIL.getId())) {
								b.setType(Material.AIR);
							}
						}
					}
				}
				if (fallingBlocks.size() == 0) {
					cleanTask = -1;
				} else {
					startTask();
				}
			}
		}, 100);
	}


	class ThrowBlockListener implements Listener {
		
		ThrowBlockSpell thisSpell;
		
		public ThrowBlockListener(ThrowBlockSpell spell) {
			this.thisSpell = spell;
		}
		
		@EventHandler(ignoreCancelled=true)
		public void onDamage(EntityDamageByEntityEvent event) {
			FallingBlockInfo info = null;
			if (removeBlocks || preventBlocks) {
				info = fallingBlocks.get(event.getDamager());
			} else {
				info = fallingBlocks.remove(event.getDamager());
			}
			if (info != null && event.getEntity() instanceof LivingEntity) {
				int damage = Math.round(event.getDamage() * info.power);
				if (callTargetEvent) {
					SpellTargetEvent evt = new SpellTargetEvent(thisSpell, info.player, (LivingEntity)event.getEntity());
					Bukkit.getPluginManager().callEvent(evt);
					if (evt.isCancelled()) {
						event.setCancelled(true);
						return;
					}
				}
				if (checkPlugins) {
					EntityDamageByEntityEvent evt = new EntityDamageByEntityEvent(info.player, event.getEntity(), DamageCause.ENTITY_ATTACK, damage);
					Bukkit.getPluginManager().callEvent(evt);
					if (evt.isCancelled()) {
						event.setCancelled(true);
						return;
					}
				}
				event.setDamage(damage);
				if (spell != null && !info.spellActivated) {
					spell.castAtLocation(info.player, event.getEntity().getLocation(), info.power);
					info.spellActivated = true;
				}
			}
		}
		
		@EventHandler(ignoreCancelled=true)
		public void onBlockLand(EntityChangeBlockEvent event) {
			if (!preventBlocks && spell == null) return;
			FallingBlockInfo info = fallingBlocks.remove(event.getEntity());
			if (info != null) {
				if (preventBlocks) {
					event.getEntity().remove();
					event.setCancelled(true);
				}
				if (spell != null && !info.spellActivated) {
					spell.castAtLocation(info.player, event.getBlock().getLocation().add(.5, .5, .5), info.power);
					info.spellActivated = true;
				}
			}
		}
	
	}
		
	@Override
	public void turnOff() {
		if (fallingBlocks != null) {
			fallingBlocks.clear();
		}
	}
	
	class FallingBlockInfo {
		Player player;
		float power;
		boolean spellActivated;
		public FallingBlockInfo(Player player, float power) {
			this.player = player;
			this.power = power;
			this.spellActivated = false;
		}
	}

}
