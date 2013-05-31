package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class MountSpell extends TargetedEntitySpell {

	public MountSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
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
				
				LivingEntity target = getTargetedPlayer(player, minRange, range, true);
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
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return false;
	}
	
	@Override
	public boolean isBeneficial() {
		return true;
	}

}
