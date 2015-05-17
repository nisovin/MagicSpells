package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.material.Vine;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;

public class VinesSpell extends TargetedSpell {

	int up;
	int down;
	int width;
	int animateInterval;
	
	public VinesSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		up = getConfigInt("up", 3);
		down = getConfigInt("down", 1);
		width = getConfigInt("width", 1);
		animateInterval = getConfigInt("animate-interval", 0);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> target = getLastTwoTargetedBlocks(player, power);
			if (target == null || target.size() != 2) {
				return noTarget(player);
			}
			if (target.get(0).getType() != Material.AIR || !target.get(1).getType().isSolid()) {
				return noTarget(player);
			}
			boolean success = growVines(player, target.get(0), target.get(1));
			if (!success) {
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean growVines(Player caster, final Block air, Block solid) {
		BlockFace face = air.getFace(solid);
		int x = 0;
		int z = 0;
		if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
			x = 1;
		} else if (face == BlockFace.EAST || face == BlockFace.WEST) {
			z = 1;
		} else {
			return false;
		}
		TreeSet<VineBlock> blocks = new TreeSet<VineBlock>();

		blocks.add(new VineBlock(air, air));
		growVinesVert(blocks, air, solid, air);
		if (width > 1) {
			for (int i = 1; i <= width / 2; i++) {
				Block a = air.getRelative(x * i, 0, z * i);
				Block s = solid.getRelative(x * i, 0, z * i);
				if (a.getType() == Material.AIR && s.getType().isSolid()) {
					blocks.add(new VineBlock(a, air));
					growVinesVert(blocks, a, s, air);
				} else {
					break;
				}
			}
			for (int i = 1; i <= width / 2; i++) {
				Block a = air.getRelative(x * -i, 0, z * -i);
				Block s = solid.getRelative(x * -i, 0, z * -i);
				if (a.getType() == Material.AIR && s.getType().isSolid()) {
					blocks.add(new VineBlock(a, air));
					growVinesVert(blocks, a, s, air);
				} else {
					break;
				}
			}
		}
		
		if (blocks.size() == 0) {
			return false;
		}
		
		if (animateInterval <= 0) {
			for (VineBlock vine : blocks) {
				setBlockToVine(vine.block, face);
			}
		} else {
			new VineAnimation(face, blocks);
		}
		return true;
	}
	
	private void setBlockToVine(Block block, BlockFace face) {
		if (block.getType() == Material.AIR) {
			BlockState state = block.getState();
			state.setType(Material.VINE);
			if (state.getData() instanceof Vine) {
				Vine data = (Vine)state.getData();
				data.putOnFace(face);
				state.setData(data);
			}
			state.update(true, false);
		}
	}
	
	private void growVinesVert(Set<VineBlock> blocks, Block air, Block solid, Block center) {
		Block b;
		for (int i = 1; i <= up; i++) {
			b = air.getRelative(0, i, 0);
			if (b.getType() == Material.AIR && solid.getRelative(0, i, 0).getType().isSolid()) {
				blocks.add(new VineBlock(b, center));
			} else {
				break;
			}
		}
		for (int i = 1; i <= down; i++) {
			b = air.getRelative(0, -i, 0);
			if (b.getType() == Material.AIR && solid.getRelative(0, -i, 0).getType().isSolid()) {
				blocks.add(new VineBlock(b, center));
			} else {
				break;
			}
		}
	}
	
	class VineBlock implements Comparable<VineBlock> {
		Block block;
		double distanceSquared;
		
		public VineBlock(Block block, Block center) {
			this.block = block;
			this.distanceSquared = block.getLocation().distanceSquared(center.getLocation());
		}
		
		@Override
		public int compareTo(VineBlock o) {
			if (o.distanceSquared < this.distanceSquared) {
				return 1;
			} else if (o.distanceSquared > this.distanceSquared) {
				return -1;
			} else {
				return o.block.getLocation().toString().compareTo(this.block.getLocation().toString());
			}
		}
		
	}
	
	class VineAnimation extends SpellAnimation {
		
		BlockFace face;
		TreeSet<VineBlock> blocks;
		
		public VineAnimation(BlockFace face, TreeSet<VineBlock> blocks) {
			super(animateInterval, true);
			this.face = face;
			this.blocks = blocks;
		}

		@Override
		protected void onTick(int tick) {
			VineBlock block = blocks.pollFirst();
			if (block != null) {
				setBlockToVine(block.block, face);
			} else {
				this.stop();
			}
		}
		
	}
	
}
