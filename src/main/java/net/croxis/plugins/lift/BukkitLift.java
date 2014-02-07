/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2013, croxis <https://github.com/croxis/>
 *
 * Lift is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Lift is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Lift. If not, see <http://www.gnu.org/licenses/>.
 */
package net.croxis.plugins.lift;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.*;

public class BukkitLift extends JavaPlugin implements Listener {
	public static boolean debug = false;
	public static boolean redstone = true;
	public static int liftArea = 16;
	public static int maxHeight = 256;
	public HashMap<Material, Double> blockSpeeds = new HashMap<Material, Double>();
	public HashSet<Material> floorMaterials = new HashSet<Material>();
	public boolean autoPlace = false;
	public boolean checkFloor = false;
	public boolean serverFlight = false;
	public boolean liftMobs = false;
	public static BukkitElevatorManager manager;
	public boolean useAntiCheat = false;
	public boolean useAntiCheat2 = false;
	public boolean useNoCheatPlus = false;
	private boolean preventEntry = false;
	public boolean preventLeave = false;
	public static String stringDestination;
	public static String stringCurrentFloor;
	public static String stringOneFloor;
	public static String stringCantEnter;
	public static String stringCantLeave;
	
	public void logDebug(String message){
		if (debug)
			this.getLogger().log(Level.INFO, "[DEBUG] " + message);
	}
	
	public void logInfo(String message){
		this.getLogger().log(Level.INFO, message);
	}
	
    public void onDisable() {
    	BukkitElevatorManager.bukkitElevators.clear();
    	getServer().getScheduler().cancelTask(BukkitElevatorManager.taskid);
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	new BukkitLiftRedstoneListener(this);
    	new BukkitLiftPlayerListener(this);
    	manager = new BukkitElevatorManager(this);
    	
    	this.getConfig().options().copyDefaults(true);
    	liftArea = this.getConfig().getInt("maxLiftArea");
    	maxHeight = this.getConfig().getInt("maxHeight");
    	debug = this.getConfig().getBoolean("debug");
    	liftMobs = this.getConfig().getBoolean("liftMobs");
    	autoPlace = this.getConfig().getBoolean("autoPlace");
    	checkFloor = this.getConfig().getBoolean("checkFloor");
    	preventEntry = this.getConfig().getBoolean("preventEntry", false);
    	preventLeave = this.getConfig().getBoolean("preventLeave", false);
    	redstone = this.getConfig().getBoolean("redstone", false);
    	Set<String> baseBlockKeys = this.getConfig().getConfigurationSection("baseBlockSpeeds").getKeys(false);
    	for (String key : baseBlockKeys){
    		blockSpeeds.put(Material.valueOf(key), this.getConfig().getDouble("baseBlockSpeeds." + key));
    	}
    	List<String> configFloorMaterials = this.getConfig().getStringList("floorBlocks");
    	for (String key : configFloorMaterials){
    		floorMaterials.add(Material.valueOf(key));
    	}
    	stringOneFloor = getConfig().getString("STRING_oneFloor", "There is only one floor silly.");
    	stringCurrentFloor = getConfig().getString("STRING_currentFloor", "Current Floor:");
    	stringDestination = getConfig().getString("STRING_dest", "Dest:");
    	stringCantEnter = getConfig().getString("STRING_cantEnter", "Can't enter elevator in use");
    	stringCantLeave = getConfig().getString("STRING_cantLeave", "Can't leave elevator in use");
    	
        saveConfig();
        
        serverFlight = this.getServer().getAllowFlight();
        
        //if (preventEntry || preventLeave){
        if (preventEntry){
        	Bukkit.getServer().getPluginManager().registerEvents(this, this);
        }
        
        if(getServer().getPluginManager().getPlugin("AntiCheat") != null)
        {
        	if (getServer().getPluginManager().getPlugin("AntiCheat").getDescription().getVersion().startsWith("2")){
        		useAntiCheat2 = true;
        		logDebug("Hooked into Anticheat 2");
        	} else {
        		useAntiCheat = true;
        		logDebug("Hooked into Anticheat 1");
        	}
        }
        if(getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
        {
          useNoCheatPlus = true;
          logDebug("Hooked into NoCheatPlus");
        }
        
        if (debug){
			System.out.println("maxArea: " + Integer.toString(liftArea));
			System.out.println("autoPlace: " + Boolean.toString(autoPlace));
			System.out.println("checkGlass: " + Boolean.toString(checkFloor));
			System.out.println("baseBlocks: " + blockSpeeds.toString());
		}
        
        try {
            BukkitMetrics bukkitMetrics = new BukkitMetrics(this);
            bukkitMetrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        
        System.out.println(this + " is now enabled!");
    }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent event){
		for (BukkitElevator bukkitElevator : BukkitElevatorManager.bukkitElevators){
			if (bukkitElevator.chunks.contains(event.getTo().getChunk())){
				if (bukkitElevator.isInShaft(event.getPlayer()) 
						&& !bukkitElevator.isInLift(event.getPlayer())
						&& preventEntry){
					event.setCancelled(true);
					event.getPlayer().sendMessage(BukkitLift.stringCantEnter);
					event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(-1));	
				} /*else if (!bukkitElevator.isInShaft(event.getPlayer())
						&& bukkitElevator.isInLift(event.getPlayer())
						&& preventLeave){
					event.setCancelled(true);
					event.getPlayer().sendMessage(BukkitLift.stringCantLeave);
				}*/
			}
		}
	}
	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	if(cmd.getName().equalsIgnoreCase("lift") && sender instanceof Player){ // If the player typed /basic then do the following...
    		long time = System.currentTimeMillis();
    		Player player = (Player) sender;
    		player.sendMessage("Starting scan");
    		BukkitElevator bukkitElevator = new BukkitElevator();
    		//Location location = player.getLocation();
    		//location.setY(location.getY() - 2);
    		if (BukkitElevatorManager.isBaseBlock(player.getLocation().getBlock().getRelative(BlockFace.DOWN))){
    			bukkitElevator.baseBlockType = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
    			BukkitElevatorManager.scanBaseBlocks(player.getLocation().getBlock().getRelative(BlockFace.DOWN), bukkitElevator);
    		} else {
    			player.sendMessage("Not a valid base block type: " + player.getLocation().getBlock().getType().toString());
    			player.sendMessage("Options are: " + this.blockSpeeds.toString());
    			return true;
    		}
    		player.sendMessage("Base block type: " + bukkitElevator.baseBlockType + " | Size: " + Integer.toString(bukkitElevator.baseBlocks.size()));
    		player.sendMessage("Floor scan reports: " + BukkitElevatorManager.constructFloors(bukkitElevator));
    		player.sendMessage("Total time generating elevator: " + Long.toString(System.currentTimeMillis() - time));
    		return true;
    	} //If this has happened the function will break and return true. if this hasn't happened the a value of false will be returned.
    	return false; 
    }


}
