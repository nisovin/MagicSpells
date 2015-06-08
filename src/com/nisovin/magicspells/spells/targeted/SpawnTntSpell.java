package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SpawnTntSpell extends TargetedSpell implements TargetedLocationSpell {

	int fuse;
	float velocity;
	float upVelocity;
	boolean cancelExplosion;
	boolean preventBlockDamage;
	
	String spellName;
	TargetedLocationSpell spell;
	
	Map<Integer, TntInfo> tnts;
	
	public SpawnTntSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		fuse = getConfigInt("fuse", 20);
		velocity = getConfigFloat("velocity", 0);
		upVelocity = getConfigFloat("up-velocity", velocity);
		cancelExplosion = getConfigBoolean("cancel-explosion", false);
		preventBlockDamage = getConfigBoolean("prevent-block-damage", false);
	}
	
	@Override
	public void initialize() {
		if (spellName != null) {
			Spell s = MagicSpells.getSpellByInternalName(spellName);
			if (s instanceof TargetedLocationSpell) {
				spell = (TargetedLocationSpell)s;
			}
			if (spell == null) {
				MagicSpells.error("Invalid spell defined on SpawnTntSpell " + internalName + ": " + spellName);
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> blocks = getLastTwoTargetedBlocks(player, power);
			if (blocks.size() == 2 && !blocks.get(0).getType().isSolid() && blocks.get(0).getType().isSolid()) {
				Location loc = blocks.get(0).getLocation().add(0.5, 0.5, 0.5);
				loc.setDirection(player.getLocation().getDirection());
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void spawnTnt(Player caster, float power, Location loc) {
		TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
		tnt.setFuseTicks(fuse);
		if (velocity > 0) {
			tnt.setVelocity(loc.getDirection().normalize().setY(0).multiply(velocity).setY(upVelocity));
		} else if (upVelocity > 0) {
			tnt.setVelocity(new Vector(0, upVelocity, 0));
		}
		tnts.put(tnt.getEntityId(), new TntInfo(caster, power));
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		target.setX(target.getBlockX() + 0.5);
		target.setY(target.getBlockY() + 0.5);
		target.setZ(target.getBlockZ() + 0.5);
		spawnTnt(caster, power, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		target.setX(target.getBlockX() + 0.5);
		target.setY(target.getBlockY() + 0.5);
		target.setZ(target.getBlockZ() + 0.5);
		spawnTnt(null, power, target);
		return true;
	}
	
	@EventHandler
	void onEntityExplode(EntityExplodeEvent event) {
		TntInfo info = tnts.remove(event.getEntity().getEntityId());
		if (info != null) {
			if (cancelExplosion) {
				event.setCancelled(true);
				event.getEntity().remove();
			}
			if (preventBlockDamage) {
				event.blockList().clear();
				event.setYield(0f);
			}
			if (spell != null) {
				if (info.caster != null) {
					if (info.caster.isValid() && !info.caster.isDead()) {
						spell.castAtLocation(info.caster, event.getEntity().getLocation(), info.power);
					}
				} else {
					spell.castAtLocation(event.getEntity().getLocation(), info.power);
				}
			}
		}
	}
	
	class TntInfo {
		Player caster;
		float power;
		public TntInfo(Player caster, float power) {
			this.caster = caster;
			this.power = power;
		}
	}

}
