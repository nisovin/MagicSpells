package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ReachSpell extends BuffSpell {

	private int range;
	private boolean consumeBlocks;
	private boolean dropBlocks;
	private List<Integer> disallowedBreakBlocks;
	private List<Integer> disallowedPlaceBlocks;
	
	private HashSet<Player> reaching;
	
	public ReachSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 15);
		consumeBlocks = getConfigBoolean("consume-blocks", true);
		dropBlocks = getConfigBoolean("drop-blocks", true);
		disallowedBreakBlocks = getConfigIntList("disallowed-break-blocks", null);
		disallowedPlaceBlocks = getConfigIntList("disallowed-place-blocks", null);
		
		reaching = new HashSet<Player>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (reaching.contains(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			reaching.add(player);
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (reaching.contains(event.getPlayer())) {
			Player player = event.getPlayer();
			
			// check expired
			if (isExpired(player)) {
				turnOff(player);
				return;
			}
			
			// get targeted block
			Action action = event.getAction();
			List<Block> targets = player.getLastTwoTargetBlocks(null, range);
			Block airBlock, targetBlock;
			if (targets.size() == 2) {
				airBlock = targets.get(0);
				targetBlock = targets.get(1);
				if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && targetBlock.getType() != Material.AIR) {
					// break
					
					// check for disallowed
					if (disallowedBreakBlocks != null && disallowedBreakBlocks.contains(targetBlock.getTypeId())) {
						return;
					}
					// call break event
					BlockBreakEvent evt = new BlockBreakEvent(targetBlock, player);
					Bukkit.getPluginManager().callEvent(evt);
					if (!evt.isCancelled()) {
						// remove block
						targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, targetBlock.getTypeId());
						// drop item
						if (dropBlocks && player.getGameMode() == GameMode.SURVIVAL) {
							targetBlock.breakNaturally();
						} else {
							targetBlock.setType(Material.AIR);
						}
						addUseAndChargeCost(player);
					}
				} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && targetBlock.getType() != Material.AIR) {
					// place
					
					// check for block in hand
					ItemStack inHand = player.getItemInHand();
					if (inHand != null && inHand.getType() != Material.AIR && inHand.getType().isBlock()) {
						
						// check for disallowed
						if (disallowedPlaceBlocks != null && disallowedPlaceBlocks.contains(inHand.getTypeId())) {
							return;
						}
						
						BlockState prevState = airBlock.getState();
						byte data = 0;
						if (inHand.getData() != null) {
							data = inHand.getData().getData();
						}
						// place block
						airBlock.setTypeIdAndData(inHand.getTypeId(), data, true);
						// call event
						BlockPlaceEvent evt = new BlockPlaceEvent(airBlock, prevState, targetBlock, inHand, player, true);
						Bukkit.getPluginManager().callEvent(evt);
						if (evt.isCancelled()) {
							// cancelled, revert
							prevState.update(true);
						} else {
							// remove item from hand
							if (consumeBlocks && player.getGameMode() == GameMode.SURVIVAL) {
								if (inHand.getAmount() > 1) {
									inHand.setAmount(inHand.getAmount() - 1);
									player.setItemInHand(inHand);
								} else {
									player.setItemInHand(null);
								}
							}
							addUseAndChargeCost(player);
						}
					}
				}
			}
		}
	}

	@Override
	public void turnOff(Player player) {
		if (reaching.contains(player)) {
			super.turnOff(player);
			reaching.remove(player);
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		reaching.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return reaching.contains(player.getName());
	}



}
