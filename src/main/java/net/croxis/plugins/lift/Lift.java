package net.croxis.plugins.lift;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import net.h31ix.anticheat.api.AnticheatAPI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Lift extends JavaPlugin implements Listener {
	public static boolean debug = false;
	boolean useSpout = false;
	public HashSet<Entity> fallers = new HashSet<Entity>();
	public HashSet<Player> flyers = new HashSet<Player>();
	//public double liftSpeed = 0.5;
	public int liftArea = 16;
	public int lowScan = 0;
	public int highScan = 255;
	//public Material baseMaterial = Material.IRON_BLOCK;
	public HashMap<Material, Double> blockSpeeds = new HashMap<Material, Double>();
	public Material floorBlock = Material.GLASS;
	public boolean autoPlace = false;
	public boolean checkGlass = false;
	public boolean serverFlight = false;
	//public boolean useV10 = false;
	//public V10verlap_API v10verlap_API = null;
	public static ElevatorManager manager;
	public boolean useAntiCheat = false;
	public AnticheatAPI anticheat = null;
	private boolean preventEntry;
	public static String stringDestination;
	public static String stringCurrentFloor;
	public static String stringOneFloor;
	public static String stringCantEnter;
	
	public void logDebug(String message){
		if (debug)
			this.getLogger().log(Level.INFO, message);
	}
	
    public void onDisable() {
    	ElevatorManager.elevators.clear();
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	new LiftRedstoneListener(this);
    	new LiftPlayerListener(this);
    	manager = new ElevatorManager(this);
    	
    	//liftSpeed = this.getConfig().getDouble("liftSpeed");
    	this.getConfig().options().copyDefaults(true);
    	liftArea = this.getConfig().getInt("maxLiftArea");
    	lowScan = this.getConfig().getInt("lowScan");
    	highScan = this.getConfig().getInt("highScan");
    	debug = this.getConfig().getBoolean("debug");
    	//baseMaterial = Material.valueOf(this.getConfig().getString("baseBlock", "IRON_BLOCK"));
    	autoPlace = this.getConfig().getBoolean("autoPlace");
    	checkGlass = this.getConfig().getBoolean("checkGlass");
    	preventEntry = this.getConfig().getBoolean("preventEntry", false);
    	Set<String> baseBlockKeys = this.getConfig().getConfigurationSection("baseBlockSpeeds").getKeys(false);
    	for (String key : baseBlockKeys){
    		blockSpeeds.put(Material.valueOf(key), this.getConfig().getDouble("baseBlockSpeeds." + key));
    	}
    	floorBlock = Material.valueOf(getConfig().getString("floorBlock"));
    	stringOneFloor = getConfig().getString("STRING_oneFloor", "There is only one floor silly.");
    	stringCurrentFloor = getConfig().getString("STRING_currentFloor", "Current Floor:");
    	stringDestination = getConfig().getString("STRING_dest", "Dest:");
    	stringCantEnter = getConfig().getString("STRING_cantEnter", "Can't enter elevator in use");
    	
        saveConfig();
        
        serverFlight = this.getServer().getAllowFlight();
        
        Plugin test = getServer().getPluginManager().getPlugin("Spout");
        
        if (preventEntry){
        	Bukkit.getServer().getPluginManager().registerEvents(this, this);
        }
        
        if(test != null) {
        	useSpout = true;
        	System.out.println(this + " detected Spout!");
        }
        
        if(getServer().getPluginManager().getPlugin("AntiCheat") != null)
        {
          useAntiCheat = true;
        }
        if (debug){
			System.out.println("maxArea: " + Integer.toString(liftArea));
			System.out.println("autoPlace: " + Boolean.toString(autoPlace));
			System.out.println("checkGlass: " + Boolean.toString(checkGlass));
			System.out.println("baseBlocks: " + blockSpeeds.toString());
		}
        
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        
        System.out.println(this + " is now enabled!");
    }
    
    /*public void removeLift(Elevator elevator){
    	
    }*/
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		for (Elevator elevator : ElevatorManager.elevators){
			if (elevator.chunks.contains(event.getTo().getChunk())){
				for (Block block : elevator.baseBlocks){
					if (block.getX() == event.getTo().getBlockX() &&
							block.getZ() == event.getTo().getBlockZ()){
						event.setCancelled(true);
						event.getPlayer().sendMessage(Lift.stringCantEnter);
					}
				}
			}
		}
	}
}
