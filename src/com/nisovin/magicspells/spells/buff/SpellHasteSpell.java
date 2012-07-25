package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SpellHasteSpell extends BuffSpell {
    
    private float castTimeModAmt;
    private float cooldownModAmt;
    
    private HashMap<String, Float> spellTimersModified;

    public SpellHasteSpell(MagicConfig config, String spellname) {
        super(config, spellname);
        
        castTimeModAmt = getConfigInt("cast-time-mod-amt", -25) / 100F;
        cooldownModAmt = getConfigInt("cooldown-mod-amt", -25) / 100F;
        
        spellTimersModified = new HashMap<String, Float>();
    }
    
    @Override
    public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {            
        if (isActive(player)){
            turnOff(player);
        }
        if (state == SpellCastState.NORMAL) {
            spellTimersModified.put(player.getName(), power);
            startSpellDuration(player);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }
    
    @EventHandler (priority=EventPriority.MONITOR)
    public void onSpellSpeedCast(SpellCastEvent event) {
    	Float power = spellTimersModified.get(event.getCaster().getName());
    	if (power != null) {
    		// modify cast time
    		if (castTimeModAmt != 0) {
	            int ct = event.getCastTime();
	            float newCT = ct + (castTimeModAmt * power * ct);
	            if (newCT < 0) newCT = 0;
	            event.setCastTime(Math.round(newCT));
    		}
    		// modify cooldown
    		if (cooldownModAmt != 0) {
	            float cd = event.getCooldown();
	            float newCD = cd + (cooldownModAmt * power * cd);
	            if (newCD < 0) newCD = 0;
	            event.setCooldown(newCD);
    		}
    	}
    }
    
    @Override
    public boolean isActive(Player player) {
        return spellTimersModified.containsKey(player.getName());
    }

    @Override
    public void turnOff(Player player) {
    	if (isActive(player)) {
    		super.turnOff(player);
    		sendMessage(player, strFade);
    		spellTimersModified.remove(player.getName());
    	}
    }

    @Override
    protected void turnOff() {
        spellTimersModified.clear();
    }
}