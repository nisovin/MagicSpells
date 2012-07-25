package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WalkwaySpell extends BuffSpell {

	private Material material;
	private int size;
	private boolean cancelOnTeleport;
	
	private HashMap<Player,Platform> platforms;
	private WalkwayListener listener;
	
	public WalkwaySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		material = Material.getMaterial(getConfigInt("platform-type", Material.WOOD.getId()));
		size = getConfigInt("size", 6);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		platforms = new HashMap<Player,Platform>();
		
	}

	public void initialize() {
		super.initialize();
		if (cancelOnTeleport) {
			registerEvents(new TeleportListener());
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (platforms.containsKey(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			platforms.put(player, new Platform(player, material, size));
			startSpellDuration(player);
			registerListener();
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void registerListener() {
		if (listener == null) {
			listener = new WalkwayListener();
			registerEvents(listener);
		}
	}
	
	private void unregisterListener() {
		if (listener != null && platforms.size() == 0) {
			unregisterEvents(listener);
			listener = null;
		}
	}
	
	public class WalkwayListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			Platform carpet = platforms.get(event.getPlayer());
			if (carpet != null) {
				boolean moved = carpet.move();
				if (moved) {
					addUseAndChargeCost(event.getPlayer());
				}
			}
		}
	
		@EventHandler(ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			for (Platform platform : platforms.values()) {
				if (platform.blockInPlatform(event.getBlock())) {
					event.setCancelled(true);
					return;
				}
			}
		}
		
	}

	public class TeleportListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			if (platforms.containsKey(event.getPlayer())) {
				if (!event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName()) || event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 50*50) {
					turnOff(event.getPlayer());
				}
			}
		}
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerPortal(PlayerPortalEvent event) {
			if (platforms.containsKey(event.getPlayer())) {
				turnOff(event.getPlayer());
			}
		}
	}
	
	@Override
	public void turnOff(Player player) {
		Platform platform = platforms.get(player);
		if (platform != null) {
			super.turnOff(player);
			platform.remove();
			platforms.remove(player);
			sendMessage(player, strFade);
			unregisterListener();
		}
	}

	@Override
	protected void turnOff() {
		for (Platform platform : platforms.values()) {
			platform.remove();
		}
		platforms.clear();
		unregisterListener();
	}
	
	private class Platform {
		
		private Player player;
		private Material material;
		private int size;
		private List<Block> platform;
		
		private int prevX;
		private int prevZ;
		private int prevDirX;
		private int prevDirY;
		private int prevDirZ;
		
		public Platform(Player player, Material material, int size) {
			this.player = player;
			this.material = material;
			this.size = size;
			this.platform = new ArrayList<Block>();
			
			move();
		}
		
		public boolean move() {
			Block origin = player.getLocation().subtract(0,1,0).getBlock();
			int x = origin.getX();
			int z = origin.getZ();
			int dirX = 0;
			int dirY = 0;
			int dirZ = 0;
			
			Vector dir = player.getLocation().getDirection().setY(0).normalize();
			if (dir.getX() > .7) {
				dirX = 1;
			} else if (dir.getX() < -.7) {
				dirX = -1;
			} else {
				dirX = 0;
			}
			if (dir.getZ() > .7) {
				dirZ = 1;
			} else if (dir.getZ() < -.7) {
				dirZ = -1;
			} else {
				dirZ = 0;
			}
			double pitch = player.getLocation().getPitch();
			if (prevDirY == 0) {
				if (pitch < -40) {
					dirY = 1;
				} else if (pitch > 40) {
					dirY = -1;
				} else {
					dirY = prevDirY;
				}
			} else if (prevDirY == 1 && pitch > -10) {
				dirY = 0;
			} else if (prevDirY == -1 && pitch < 10) {
				dirY = 0;
			} else {
				dirY = prevDirY;
			}
			
			if (x != prevX || z != prevZ || dirX != prevDirX || dirY != prevDirY || dirZ != prevDirZ) {
				
				if (origin.getType() == Material.AIR) {
					// check for weird stair positioning
					Block up = origin.getRelative(0,1,0);
					if (up != null && ((material == Material.WOOD && up.getType() == Material.WOOD_STAIRS) || (material == Material.COBBLESTONE && up.getType() == Material.COBBLESTONE_STAIRS))) {
						origin = up;
					} else {					
						// allow down movement when stepping out over an edge
						Block down = origin.getRelative(0,-1,0);
						if (down != null && down.getType() != Material.AIR) {
							origin = down;
						}
					}
				}
				
				drawCarpet(origin, dirX, dirY, dirZ);
				
				prevX = x;
				prevZ = z;
				prevDirX = dirX;
				prevDirY = dirY;
				prevDirZ = dirZ;
				
				return true;
			}
			
			return false;
		}
		
		public boolean blockInPlatform(Block block) {
			return platform.contains(block);
		}
		
		public void remove() {
			for (Block b : platform) {
				b.setType(Material.AIR);
			}
		}
		
		public void drawCarpet(Block origin, int dirX, int dirY, int dirZ) {
			// determine block type and maybe stair direction
			int mat = material.getId();
			byte data = 0;
			if ((material == Material.WOOD || material == Material.COBBLESTONE) && dirY != 0) {
				boolean changed = false;
				if (dirY == -1) {
					if (dirX == -1 && dirZ == 0) {
						data = 0;
						changed = true;
					} else if (dirX == 1 && dirZ == 0) {
						data = 1;
						changed = true;
					} else if (dirZ == -1 && dirX == 0) {
						data = 2;
						changed = true;
					} else if (dirZ == 1 && dirX == 0) {
						data = 3;
						changed = true;
					}
				} else if (dirY == 1) {
					if (dirX == -1 && dirZ == 0) {
						data = 1;
						changed = true;
					} else if (dirX == 1 && dirZ == 0) {
						data = 0;
						changed = true;
					} else if (dirZ == -1 && dirX == 0) {
						data = 3;
						changed = true;
					} else if (dirZ == 1 && dirX == 0) {
						data = 2;
						changed = true;
					}
				}
				if (changed) {
					if (material == Material.WOOD) {
						mat = Material.WOOD_STAIRS.getId();
					} else if (material == Material.COBBLESTONE) {
						mat = Material.COBBLESTONE_STAIRS.getId();
					}
				}
			}
			
			// get platform blocks
			List<Block> blocks = new ArrayList<Block>();
			blocks.add(origin); // add standing block
			for (int i = 1; i < size; i++) { // add blocks ahead
				Block b = origin.getRelative(dirX*i, dirY*i, dirZ*i);
				if (b != null) {
					blocks.add(b);
				}
			}
			
			// remove old blocks
			Iterator<Block> iter = platform.iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				if (!blocks.contains(b)) {
					b.setType(Material.AIR);
					iter.remove();
				}
			}
			
			// set new blocks
			for (Block b : blocks) {
				if (platform.contains(b) || b.getType() == Material.AIR) {
					b.setTypeIdAndData(mat, data, false);
					platform.add(b);
				}
			}
		}
		
	}

	@Override
	public boolean isActive(Player player) {
		return platforms.containsKey(player);
	}

}
