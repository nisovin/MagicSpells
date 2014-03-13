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
    public boolean castBuff(Player player, float power, String[] args) {    
        spellTimersModified.put(player.getName(), power);
        return true;
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
    public void turnOffBuff(Player player) {
    	spellTimersModified.remove(player.getName());
    }

    @Override
    protected void turnOff() {
        spellTimersModified.clear();
    }
}