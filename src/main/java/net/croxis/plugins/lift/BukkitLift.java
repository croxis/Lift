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
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

public class BukkitLift extends JavaPlugin implements Listener {
	public static BukkitElevatorManager manager;
	static BukkitConfig config = new BukkitConfig();

	public Double getBlockSpeed(Material material) {
	    try {
            return BukkitConfig.blockSpeeds.get(material);
        } catch (Exception e) {
	        this.getLogger().warning("There was an exception getting the block speed for " + material.toString());
	        return 0.0;
        }
    }
	
	public void logDebug(String message){
		if (BukkitConfig.debug)
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
        BukkitConfig.liftArea = this.getConfig().getInt("maxLiftArea");
        BukkitConfig.maxHeight = this.getConfig().getInt("maxHeight");
        BukkitConfig.debug = this.getConfig().getBoolean("debug");
        BukkitConfig.liftMobs = this.getConfig().getBoolean("liftMobs");
        BukkitConfig.autoPlace = this.getConfig().getBoolean("autoPlace");
        BukkitConfig.checkFloor = this.getConfig().getBoolean("checkFloor", false);
        BukkitConfig.preventEntry = this.getConfig().getBoolean("preventEntry", false);
        BukkitConfig.preventLeave = this.getConfig().getBoolean("preventLeave", false);
        BukkitConfig.redstone = this.getConfig().getBoolean("redstone", false);
    	Set<String> baseBlockKeys = this.getConfig().getConfigurationSection("baseBlockSpeeds").getKeys(false);
    	for (String key : baseBlockKeys){
            BukkitConfig.blockSpeeds.put(Material.valueOf(key), this.getConfig().getDouble("baseBlockSpeeds." + key));
    	}
    	List<String> configFloorMaterials = this.getConfig().getStringList("floorBlocks");
    	for (String key : configFloorMaterials){
            BukkitConfig.floorMaterials.add(Material.valueOf(key));
    	}
        BukkitConfig.stringOneFloor = getConfig().getString("STRING_oneFloor", "There is only one floor silly.");
        BukkitConfig.stringCurrentFloor = getConfig().getString("STRING_currentFloor", "Current Floor:");
        BukkitConfig.stringDestination = getConfig().getString("STRING_dest", "Dest:");
        BukkitConfig.stringCantEnter = getConfig().getString("STRING_cantEnter", "Can't enter elevator in use");
        BukkitConfig.stringCantLeave = getConfig().getString("STRING_cantLeave", "Can't leave elevator in use");

        BukkitConfig.metricbool = getConfig().getBoolean("metrics", true);

        saveConfig();

        BukkitConfig.serverFlight = this.getServer().getAllowFlight();
        
        //if (preventEntry || preventLeave){
        if (BukkitConfig.preventEntry){
        	Bukkit.getServer().getPluginManager().registerEvents(this, this);
        }
        
        if(getServer().getPluginManager().getPlugin("NoCheatPlus") != null){
            BukkitConfig.useNoCheatPlus = true;
          logDebug("Hooked into NoCheatPlus");
        }
        
        logDebug("maxArea: " + Integer.toString(BukkitConfig.liftArea));
        logDebug("autoPlace: " + Boolean.toString(BukkitConfig.autoPlace));
        logDebug("checkGlass: " + Boolean.toString(BukkitConfig.checkFloor));
        logDebug("baseBlocks: " + BukkitConfig.blockSpeeds.toString());
        logDebug("floorBlocks: " + BukkitConfig.floorMaterials.toString());
        
        System.out.println(this + " is now enabled!");

        if (BukkitConfig.metricbool){
            try {
                Metrics metrics = new Metrics(this);
                Graph numberofpassengers = metrics.createGraph("Number of passengers");
                metrics.start();
            } catch (IOException e) {
                logDebug(e.getMessage());
            }
        }
    }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent event){
		for (BukkitElevator bukkitElevator : BukkitElevatorManager.bukkitElevators){
			if (bukkitElevator.chunks.contains(event.getTo().getChunk())){
				if (bukkitElevator.isInShaft(event.getPlayer()) 
						&& !bukkitElevator.isInLift(event.getPlayer())
						&& BukkitConfig.preventEntry){
					event.setCancelled(true);
					event.getPlayer().sendMessage(BukkitConfig.stringCantEnter);
					event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(-1));	
				}
			}
		}
	}
	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	if(cmd.getName().equalsIgnoreCase("lift") && sender instanceof Player){ // If the player typed /basic then do the following...
    		long time = System.currentTimeMillis();
    		Player player = (Player) sender;
    		player.sendMessage("Starting scan");
    		BukkitElevator bukkitElevator = new BukkitElevator();
    		if (BukkitElevatorManager.isBaseBlock(player.getLocation().getBlock().getRelative(BlockFace.DOWN))){
    			bukkitElevator.baseBlockType = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
    			BukkitElevatorManager.scanBaseBlocks(player.getLocation().getBlock().getRelative(BlockFace.DOWN), bukkitElevator);
    		} else {
    			player.sendMessage("Not a valid base block type: " + player.getLocation().getBlock().getType().toString());
    			player.sendMessage("Options are: " + BukkitConfig.blockSpeeds.toString());
    			return true;
    		}
    		player.sendMessage("Base block type: " + bukkitElevator.baseBlockType + " | Size: " + Integer.toString(bukkitElevator.baseBlocks.size()));
    		player.sendMessage("Floor scan reports: " + BukkitElevatorManager.constructFloors(bukkitElevator));
    		player.sendMessage("Total time generating elevator: " + Long.toString(System.currentTimeMillis() - time));
    		return true;
    	}
    	return false; 
    }


}
