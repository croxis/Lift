package net.croxis.plugins.lift;

import java.util.ArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lift extends JavaPlugin {
	boolean debug = false;
	private final LiftRedstoneListener redstoneListener = new LiftRedstoneListener(this);
	private final LiftPlayerListener playerListener = new LiftPlayerListener(this);
	public ArrayList<Player> fallers = new ArrayList<Player>();
    public void onDisable() {
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	EntityListener entityListener = new EntityListener(){
    		@Override
    		public void onEntityDamage(EntityDamageEvent e){
    			if(e.getCause() == DamageCause.FALL){
    				Entity fallerE = e.getEntity();
    				if (fallerE instanceof Player){
    					Player faller = (Player) fallerE;
    					if(fallers.contains(faller)){
    						e.setCancelled(true);
    						fallers.remove(faller);
    					}
    				}
    			}
    		}
		};
    	
    	PluginManager pm = getServer().getPluginManager();
    	pm.registerEvent(Event.Type.REDSTONE_CHANGE, this.redstoneListener, Event.Priority.Low, this);
    	pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, Event.Priority.Low, this);
    	pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.High, this);
        System.out.println(this + " is now enabled!");
    }
}
