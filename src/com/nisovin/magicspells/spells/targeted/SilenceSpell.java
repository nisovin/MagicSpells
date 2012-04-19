package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SilenceSpell extends TargetedEntitySpell {

	private int duration;
	private boolean obeyLos;
	private String strSilenced;
	
	private HashMap<String,Unsilencer> silenced;
	
	public SilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		duration = getConfigInt("duration", 200);
		obeyLos = getConfigBoolean("obey-los", true);
		strSilenced = getConfigString("str-silenced", "You are silenced!");
		
		silenced = new HashMap<String,Unsilencer>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Player target = getTargetedPlayer(player, range, obeyLos);
			if (target == null) {
				return noTarget(player);
			}
						
			// silence player
			silence(target, power);
			
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void silence(Player player, float power) {
		// handle previous silence
		Unsilencer u = silenced.get(player.getName());
		if (u != null) {
			u.cancel();
		}
		// silence now
		silenced.put(player.getName(), new Unsilencer(player, Math.round(duration * power)));
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			silence((Player)target, power);
			return true;
		} else {
			return false;
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onSpellCast(SpellCastEvent event) {
		if (silenced.containsKey(event.getCaster().getName())) {
			event.setCancelled(true);
			sendMessage(event.getCaster(), strSilenced);
		}
	}
	
	public class Unsilencer implements Runnable {

		private String playerName;
		private boolean canceled = false;
		private int taskId = -1;
		
		public Unsilencer(Player player, int delay) {
			this.playerName = player.getName();
			taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, this, delay);
		}
		
		@Override
		public void run() {
			if (!canceled) {
				silenced.remove(playerName);
			}
		}
		
		public void cancel() {
			canceled = true;
			if (taskId > 0) {
				Bukkit.getScheduler().cancelTask(taskId);
			}
		}
		
	}

}
