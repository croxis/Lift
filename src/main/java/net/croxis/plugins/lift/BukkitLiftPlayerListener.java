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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BukkitLiftPlayerListener implements Listener{
	private BukkitLift plugin;
	private Block buttonBlock = null;

	public BukkitLiftPlayerListener(BukkitLift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event){
		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) 
				&& event.getPlayer().hasPermission("lift.change")) {
			buttonBlock = event.getClickedBlock().getRelative(BlockFace.DOWN);

			if (event.getClickedBlock().getType() == Material.WALL_SIGN
                && buttonBlock != null
			    && BukkitElevatorManager.isButton(buttonBlock)) {

				Sign sign = (Sign) event.getClickedBlock().getState();
				BukkitElevator bukkitElevator = BukkitElevatorManager.createLift(buttonBlock, event.getPlayer().getName());
				
				if (bukkitElevator == null){
					plugin.logDebug("Player elevator generation returned a null object. Button block at: " + buttonBlock.getLocation().toString());
					plugin.logDebug("Please see previous messages to determine why.");
					return;
				}
				
				if (bukkitElevator.getTotalFloors() < 1){
					// This is just a button and sign, not an elevator.
					return;
				} else if (bukkitElevator.getTotalFloors() == 1){
					event.getPlayer().sendMessage(BukkitConfig.stringOneFloor);
					return;
				}
				
				event.setCancelled(true);
				
				int currentDestinationInt = 1;
				BukkitFloor currentFloor = bukkitElevator.getFloorFromY(buttonBlock.getY());
				if (currentFloor == null){
					event.getPlayer().sendMessage("Elevator generator says this floor does not exist. Check shaft for blockage");
					return;
				}
				
				String sign0 = BukkitConfig.stringCurrentFloor;
				String sign1 = Integer.toString(currentFloor.getFloor());
				String sign2 = "";
				String sign3 = "";
				try{
					String[] splits = sign.getLine(2).split(": ");
					currentDestinationInt = Integer.parseInt(splits[1]);	
				} catch (Exception e){
					currentDestinationInt = 0;
					plugin.logDebug("Non valid previous destination");
				}
				currentDestinationInt++;
				if (currentDestinationInt == currentFloor.getFloor()){
					currentDestinationInt++;
					plugin.logDebug("Skipping current floor");
				}
				// The following line MAY be what causes a potential bug for max floors
				if (currentDestinationInt > bukkitElevator.getTotalFloors()){
					currentDestinationInt = 1;
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					plugin.logDebug("Rotating back to first floor");
				}
				sign2 = BukkitConfig.stringDestination + " " + Integer.toString(currentDestinationInt);
				sign3 = bukkitElevator.getFloorFromN(currentDestinationInt).getName();
				sign.setLine(0, sign0 + ChatColor.RESET);
				sign.setLine(1, sign1 + ChatColor.RESET);
				sign.setLine(2, sign2 + ChatColor.RESET);
				sign.setLine(3, sign3 + ChatColor.RESET);
				sign.update();
				plugin.logDebug("Completed sign update");
			}
		}
	}
	
	@EventHandler
	public void onPlayerItemPickup(EntityPickupItemEvent event){
		if (BukkitElevatorManager.isPassenger(event.getItem()))
			BukkitElevatorManager.removePassenger(event.getItem());
	}
	
	@EventHandler
	public void onItemPickup(InventoryPickupItemEvent event){
		if (BukkitElevatorManager.isPassenger(event.getItem()))
			BukkitElevatorManager.removePassenger(event.getItem());
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e){
		if(e.getCause() == DamageCause.FALL){
			Entity faller = e.getEntity();
			if(BukkitElevatorManager.fallers.contains(faller)){
				e.setCancelled(true);
				BukkitElevatorManager.fallers.remove(faller);
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		BukkitElevatorManager.removePlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event){
		BukkitElevatorManager.removePlayer(event.getPlayer());
	}

}
