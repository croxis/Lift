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

import java.util.logging.Level;

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
	static BukkitElevatorManager manager;
	static BukkitConfig config = new BukkitConfig();

	Double getBlockSpeed(Material material) {
	    try {
            return BukkitConfig.blockSpeeds.get(material);
        } catch (Exception e) {
	        this.getLogger().warning("There was an exception getting the block speed for " + material.toString());
	        return 0.0;
        }
    }
	
	void logDebug(String message){
		if (BukkitConfig.debug)
			this.getLogger().log(Level.INFO, "[DEBUG] " + message);
	}
	
	void logInfo(String message){
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
    	
    	config.loadConfig(this);
        
        logDebug("maxArea: " + Integer.toString(BukkitConfig.liftArea));
        logDebug("autoPlace: " + Boolean.toString(BukkitConfig.autoPlace));
        logDebug("checkGlass: " + Boolean.toString(BukkitConfig.checkFloor));
        logDebug("baseBlocks: " + BukkitConfig.blockSpeeds.toString());
        logDebug("floorBlocks: " + BukkitConfig.floorMaterials.toString());
        
        System.out.println(this + " is now enabled!");
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
    	if(cmd.getName().equalsIgnoreCase("lift")
                && sender instanceof Player
                && args.length == 0){ // If the player typed /basic then do the following...
    		long time = System.currentTimeMillis();
    		Player player = (Player) sender;
    		player.sendMessage("Starting scan");
    		BukkitElevator bukkitElevator = new BukkitElevator(this);
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
    		player.sendMessage("Total time generating elevator: " + (System.currentTimeMillis() - time));
    		return true;
    	} else if(cmd.getName().equalsIgnoreCase("lift") && args[0].equalsIgnoreCase("reload")) {
            BukkitElevatorManager.quickEndLifts();
            BukkitElevatorManager.clear();

            config = new BukkitConfig();
            config.loadConfig(this);
    	    return true;
        }
    	return false; 
    }


}
