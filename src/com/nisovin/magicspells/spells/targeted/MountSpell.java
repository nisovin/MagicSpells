package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class MountSpell extends TargetedSpell implements TargetedEntitySpell {

	boolean reverse = false;
	
	public MountSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		reverse = getConfigBoolean("reverse", false);
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (!reverse) {
				// normal - casting player mounts target
				if (player.getVehicle() != null) {
					// leave stack
					Entity veh = player.getVehicle();
					veh.eject();
					Entity pass = player.getPassenger();
					if (pass != null) {
						player.eject();
						veh.setPassenger(pass);
					}
				} else {
					// join stack
					LivingEntity target = null;
					TargetInfo<Player> targetInfo = getTargetedPlayer(player, power);
					if (targetInfo != null) {
						target = targetInfo.getTarget();
					}
					if (target != null) {
						while (target.getPassenger() != null && target.getPassenger() instanceof LivingEntity) {
							target = (LivingEntity)target.getPassenger();
						}
						player.eject();
						target.setPassenger(player);
						sendMessages(player, target);
						return PostCastAction.NO_MESSAGES;
					} else {
						return noTarget(player);
					}
				}
			} else {
				// reverse - casting player forces target to mount self
				LivingEntity target = null;
				TargetInfo<Player> targetInfo = getTargetedPlayer(player, power);
				if (targetInfo != null) {
					target = targetInfo.getTarget();
				}
				if (target != null) {
					// clear out any previous passengers
					if (player.getPassenger() != null) {
						player.eject();
					}
					if (player.getVehicle() != null) {
						player.getVehicle().eject();
					}
					if (target.getPassenger() != null) {
						target.eject();
					}
					if (target.getVehicle() != null) {
						target.getVehicle().eject();
					}
					// set passenger
					player.setPassenger(target);
					sendMessages(player, target);
					return PostCastAction.NO_MESSAGES;
				}
			}			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	@Override
	public boolean isBeneficialDefault() {
		return true;
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Entity vehicle = player.getVehicle();
		Entity passenger = player.getPassenger();
		if (passenger != null) {
			player.eject();
		}
		if (vehicle != null && vehicle instanceof Player) {
			vehicle.eject();
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Entity vehicle = player.getVehicle();
		Entity passenger = player.getPassenger();
		if (passenger != null) {
			player.eject();
		}
		if (vehicle != null && vehicle instanceof Player) {
			vehicle.eject();
		}
	}

}
