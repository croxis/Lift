package net.croxis.plugins.lift;

import java.util.ArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lift extends JavaPlugin {
	boolean debug = false;
	private final LiftRedstoneListener redstoneListener = new LiftRedstoneListener(this);
	private final LiftPlayerListener playerListener = new LiftPlayerListener(this);
	public ArrayList<Entity> fallers = new ArrayList<Entity>();
	public ArrayList<Elevator> lifts = new ArrayList<Elevator>();
	public double liftSpeed = 0.5;
	public int liftArea = 16;
    public void onDisable() {
    	lifts.clear();
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
		
		PlayerListener playerListener = new PlayerListener(){
			@Override
			public void onPlayerQuit(PlayerQuitEvent e){
				for (Elevator elevator : lifts){
					if (elevator.passengers.contains(e.getPlayer())){
						elevator.passengers.remove(e.getPlayer());
					}
				}
			}
		};
		//playerListener.onPlayerQuit(event)
    	
    	PluginManager pm = getServer().getPluginManager();
    	pm.registerEvent(Event.Type.REDSTONE_CHANGE, this.redstoneListener, Event.Priority.Low, this);
    	pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, Event.Priority.Low, this);
    	pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.High, this);
    	pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
    	liftSpeed = this.getConfig().getDouble("liftSpeed");
    	liftArea = this.getConfig().getInt("maxLiftArea");
    	this.debug = this.getConfig().getBoolean("debug");
    	this.getConfig().options().copyDefaults(true);
        saveConfig();
        System.out.println(this + " is now enabled!");
    }
}
