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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BukkitLiftPlayerListener implements Listener{
	private BukkitLift plugin;
	private Block buttonBlock = null;
	private Map<BukkitElevator, UUID> elevatorCache = new HashMap<>();
	private Map<UUID, BukkitElevator> playerCache = new HashMap<>();
	private Map<UUID, LiftSign> signCache = new HashMap<>();
	private Map<UUID, Sign> otherSignCache = new HashMap<>();

	public BukkitLiftPlayerListener(BukkitLift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event){
		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) 
				&& event.getPlayer().hasPermission("lift.change")
				//&& !playerCache.containsKey(event.getPlayer().getUniqueId())
			) {
			buttonBlock = event.getClickedBlock().getRelative(BlockFace.DOWN);

			if (BukkitConfig.signMaterials.contains(event.getClickedBlock().getType())
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

				// Located here in case sign above button is for another mod/plugin
				LiftSign liftSign = new LiftSign(BukkitLift.config, sign.getLines());
				
				// If sign is clicked with an item in hand, just switch to the next floor
				// If a sign is clicked with an open hand, use the mouse scroll method
				plugin.logDebug("HAND: " + event.getPlayer().getInventory().getItemInMainHand().toString());
				if (playerCache.containsKey(event.getPlayer().getUniqueId())){
					plugin.logDebug("HAND: REMOVE");
					removePlayerCache(event.getPlayer());
					event.setCancelled(true);
					event.getPlayer().sendMessage(BukkitConfig.stringScrollSelectDisabled);
					return;
				}
				if ((event.getPlayer().getInventory().getItemInMainHand() == null
						|| event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR)
						&& !liftSign.isEmpty()) {
					plugin.logDebug("HAND: ADD");
					elevatorCache.put(bukkitElevator, event.getPlayer().getUniqueId());
					playerCache.put(event.getPlayer().getUniqueId(), bukkitElevator);
					signCache.put(event.getPlayer().getUniqueId(), liftSign);
					otherSignCache.put(event.getPlayer().getUniqueId(), sign);
					event.getPlayer().sendMessage(BukkitConfig.stringScrollSelectEnabled);
				} else {
					plugin.logDebug("FULL HAND CYCLE");
					int currentDestinationInt = 1;
					BukkitFloor currentFloor = bukkitElevator.getFloorFromY(buttonBlock.getY());
					if (currentFloor == null) {
						event.getPlayer().sendMessage("Elevator generator says this floor does not exist. Check shaft for blockage");
						return;
					}

					liftSign.setCurrentFloor(currentFloor.getFloor());
					currentDestinationInt = liftSign.getDestinationFloor();
					currentDestinationInt++;
					if (currentDestinationInt == currentFloor.getFloor()) {
						currentDestinationInt++;
						plugin.logDebug("Skipping current floor");
					}
					// The following line MAY be what causes a potential bug for max floors
					if (currentDestinationInt > bukkitElevator.getTotalFloors()) {
						currentDestinationInt = 1;
						if (currentFloor.getFloor() == 1)
							currentDestinationInt = 2;
						plugin.logDebug("Rotating back to first floor");
					}
					liftSign.setDestinationFloor(currentDestinationInt);
					liftSign.setDestinationName(bukkitElevator.getFloorFromN(currentDestinationInt).getName());
					String[] data = liftSign.saveSign();
					sign.setLine(0, data[0]);
					sign.setLine(1, data[1]);
					sign.setLine(2, data[2]);
					sign.setLine(3, data[3]);
					sign.update();
					plugin.logDebug("Completed sign update");
				}
			}
		}
	}

	@EventHandler
	public void onHeldItemChange(PlayerItemHeldEvent event){
		if (!playerCache.containsKey(event.getPlayer().getUniqueId()))
			return;

		int isForwards;
		// This is from Craftbook
		if (event.getNewSlot() > event.getPreviousSlot()) {
			isForwards = 1;
		} else if (event.getNewSlot() < event.getPreviousSlot()) {
			isForwards = -1;
		} else return;
		if (event.getPreviousSlot() == 0 && event.getNewSlot() == 8) {
			isForwards = -1;
		} else if (event.getPreviousSlot() == 8 && event.getNewSlot() == 0) {
			isForwards = 1;
		}

		BukkitElevator bukkitElevator = playerCache.get(event.getPlayer().getUniqueId());
		LiftSign liftSign = signCache.get(event.getPlayer().getUniqueId());
		Sign sign = otherSignCache.get(event.getPlayer().getUniqueId());

		if (event.getPlayer().getLocation().distance(sign.getLocation()) > 3) {
			removePlayerCache(event.getPlayer());
			event.getPlayer().sendMessage(BukkitConfig.stringScrollSelectDisabled);
			return;
		}

		BukkitFloor currentFloor = bukkitElevator.getFloorFromY(buttonBlock.getY());
		if (currentFloor == null) {
			event.getPlayer().sendMessage("Elevator generator says this floor does not exist. Check shaft for blockage");
			return;
		}

		liftSign.setCurrentFloor(currentFloor.getFloor());
		int currentDestinationInt = liftSign.getDestinationFloor();
		currentDestinationInt += isForwards;
		if (currentDestinationInt == currentFloor.getFloor()) {
			currentDestinationInt += isForwards;
			plugin.logDebug("Skipping current floor");
		}
		// The following line MAY be what causes a potential bug for max floors
		if (currentDestinationInt > bukkitElevator.getTotalFloors()) {
			currentDestinationInt = 1;
			if (currentFloor.getFloor() == 1)
				currentDestinationInt = 2;
			plugin.logDebug("Rotating back to first floor");
		}
		if (currentDestinationInt < 1) {
			currentDestinationInt = bukkitElevator.getTotalFloors();
			if (currentFloor.getFloor() == bukkitElevator.getTotalFloors())
				currentDestinationInt = bukkitElevator.getTotalFloors() - 1;
			plugin.logDebug("Rotating back to top floor");
		}

		liftSign.setDestinationFloor(currentDestinationInt);
		liftSign.setDestinationName(bukkitElevator.getFloorFromN(currentDestinationInt).getName());
		String[] data = liftSign.saveSign();
		sign.setLine(0, data[0]);
		sign.setLine(1, data[1]);
		sign.setLine(2, data[2]);
		sign.setLine(3, data[3]);
		sign.update();
		plugin.logDebug("Completed sign update");

	}
	
	@EventHandler
	public void onPlayerItemPickup(EntityPickupItemEvent event){
		if (BukkitElevatorManager.isInALift(event.getItem()))
			BukkitElevatorManager.removePassenger(event.getItem());
	}
	
	@EventHandler
	public void onItemPickup(InventoryPickupItemEvent event){
		if (BukkitElevatorManager.isInALift(event.getItem()))
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
		removePlayerCache(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event){
		BukkitElevatorManager.removePlayer(event.getPlayer());
		removePlayerCache(event.getPlayer());
	}

	@EventHandler
	public void onElevatorActivation(ElevatorActivateEvent event) {
		playerCache.entrySet()
				.stream()
				.filter(entry -> entry.getValue().equals(event.getElevator()))
				.map(entry -> Bukkit.getPlayer(entry.getKey()))
				.findFirst()
				.ifPresent(this::removePlayerCache);
	}

	void removePlayerCache(Player player){
		playerCache.remove(player.getUniqueId());
		signCache.remove(player.getUniqueId());
		otherSignCache.remove(player.getUniqueId());
		// https://www.geeksforgeeks.org/remove-an-entry-using-value-from-hashmap-while-iterating-over-it/
		elevatorCache.entrySet().removeIf(entry -> player.getUniqueId().equals(entry.getValue()));
	}

}
