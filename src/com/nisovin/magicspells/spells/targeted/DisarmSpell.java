package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class DisarmSpell extends TargetedEntitySpell {

	private List<Integer> disarmable;
	private int disarmDuration;
	private boolean obeyLos;
	
	private String strNoTarget;
	private String strInvalidItem;
	private String strCastTarget;
	
	public DisarmSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		disarmable = new ArrayList<Integer>();
		disarmable.add(280);
		disarmable = getConfigIntList("disarmable-items", disarmable);
		
		disarmDuration = getConfigInt("disarm-duration", 100);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
		strInvalidItem = getConfigString("str-invalid-item", "Your target could not be disarmed.");
		strCastTarget = getConfigString("str-cast-target", "%a has disarmed you.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			Player target = getTargetedPlayer(player, range, obeyLos);
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			boolean disarmed = disarm(target);
			if (disarmed) {
				// send messages
				sendMessage(player, strCastSelf, "%t", target.getDisplayName());
				sendMessage(target, strCastTarget, "%a", player.getDisplayName());
				sendMessageNear(player, formatMessage(strCastOthers, "%t", target.getDisplayName(), "%a", player.getDisplayName()));
				return PostCastAction.NO_MESSAGES;
			} else {
				// fail
				sendMessage(player, strInvalidItem);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean disarm(Player target) {
		ItemStack inHand = target.getItemInHand();
		if (disarmable.contains(inHand.getTypeId())) {
			// drop item
			target.setItemInHand(null);
			Item item = target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
			item.setPickupDelay(disarmDuration);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			return disarm((Player)target);
		} else {
			return false;
		}
	}

}
