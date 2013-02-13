/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2012, croxis <https://github.com/croxis/>
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

import java.util.ArrayList;
import java.util.Iterator;

import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.block.Sign;
import org.getspout.spoutapi.SpoutManager;


public class BukkitLiftRedstoneListener implements Listener {
	private final BukkitLift plugin;
	// Supporting annoying out of date servers
	private boolean canDo = false;
	public BukkitLiftRedstoneListener(BukkitLift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	} 
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		BukkitElevator bukkitElevator = null;
		canDo = false;
		if(Material.getMaterial("WOOD_BUTTON") != null)
			canDo = (event.getBlock().getType() == Material.STONE_BUTTON || event.getBlock().getType() == Material.WOOD_BUTTON) 
					&& (!event.getBlock().isBlockIndirectlyPowered())
					&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN;
		else
			canDo = event.getBlock().getType() == Material.STONE_BUTTON 
					&& (!event.getBlock().isBlockIndirectlyPowered())
					&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN;
		/*if ((event.getBlock().getType() == Material.STONE_BUTTON || event.getBlock().getType() == Material.WOOD_BUTTON) 
				&& (!event.getBlock().isBlockIndirectlyPowered())
				&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN){*/
		if (canDo){
			long startTime = System.currentTimeMillis();
			//plugin.logDebug("Initial reqs met");
			//elevator = new Elevator(this.plugin, event.getBlock());
			bukkitElevator = BukkitElevatorManager.createLift(event.getBlock());
			if (bukkitElevator == null)
				return;
			
			//See if lift is in use
			for (BukkitElevator e : BukkitElevatorManager.bukkitElevators){
				Iterator<Block> iterator = bukkitElevator.baseBlocks.iterator();
				while (iterator.hasNext()){
					if (e.baseBlocks.contains(iterator.next()))
						return;
				}
			}
			
			if (bukkitElevator.getTotalFloors() < 2)
				return;
			
			int y = event.getBlock().getY();
			Floor startFloor = bukkitElevator.floormap.get(y);
			bukkitElevator.startFloor = startFloor;
			String line = ((Sign) event.getBlock().getRelative(BlockFace.UP).getState()).getLine(2);
			if (line.isEmpty())
				return;
			String[] splits = line.split(": ");
			if (splits.length != 2)
				return;
			int destination = Integer.parseInt(splits[1]);	

			//Get all players in elevator shaft (at floor of button pusher if possible)
			//And set their gravity to 0
			bukkitElevator.destFloor = bukkitElevator.getFloorFromN(destination);
			
			if (BukkitLift.debug){
				System.out.println("Elevator start floor:" + startFloor.getFloor());
				System.out.println("Elevator start floor y:" + startFloor.getY());
				System.out.println("Elevator destination floor:" + destination);
				System.out.println("Elevator destination y:" + bukkitElevator.destFloor.getY());
			}
			
			Iterator<Block> iterator = bukkitElevator.baseBlocks.iterator();
			for(Chunk chunk : bukkitElevator.chunks){
				plugin.logDebug("Number of entities in this chunk: " + Integer.toString(chunk.getEntities().length));
				for(Entity e : chunk.getEntities()){
					if (e instanceof LivingEntity){
						if (bukkitElevator.isInShaftAtFloor(e, startFloor)){
							if (BukkitElevatorManager.isPassenger(e)){
								if (e instanceof Player)
									((Player) e).sendMessage("You are already in a lift. Relog in case this is an error.");
								continue;
							}
							bukkitElevator.addPassenger((LivingEntity) e);
							if (iterator.hasNext() && plugin.autoPlace){
								Location loc = iterator.next().getLocation();
								e.teleport(new Location(e.getWorld(), loc.getX() + 0.5D, e.getLocation().getY(), loc.getZ() + 0.5D, e.getLocation().getYaw(), e.getLocation().getPitch()));
							}
							if (e instanceof Player){
								Player player = (Player) e;
								if (player.getAllowFlight()){
									BukkitElevatorManager.flyers.add(player);
									plugin.logDebug(player.getName() + " added to flying list");
								} else {
                                    BukkitElevatorManager.flyers.remove(player);
                                    //player.setAllowFlight(false);
                                    plugin.logDebug(player.getName() + " NOT added to flying list");
                                }
								plugin.logDebug("Flyers: " + BukkitElevatorManager.flyers.toString());
								if (!player.hasPermission("lift")){
									bukkitElevator.holders.put((LivingEntity) e, e.getLocation());
									bukkitElevator.passengers.remove((LivingEntity) e);
								}
							}
						} else if (!bukkitElevator.isInShaftAtFloor(e, startFloor) && bukkitElevator.isInShaft(e)){
							bukkitElevator.holders.put((LivingEntity) e, e.getLocation());
							bukkitElevator.passengers.remove((LivingEntity) e);
						}
					}
				}
			}
			
			//Disable all glass inbetween players and destination
			ArrayList<Floor> glassfloors = new ArrayList<Floor>();
			//Going up
			if (startFloor.getY() < bukkitElevator.destFloor.getY()){
				for(int i = startFloor.getFloor() + 1; i<= bukkitElevator.destFloor.getFloor(); i++){
					glassfloors.add(bukkitElevator.floormap2.get(i));
				}
			}
			//Going down
			else {
				for(int i = bukkitElevator.destFloor.getFloor() + 1; i<= startFloor.getFloor(); i++){
					glassfloors.add(bukkitElevator.floormap2.get(i));
				}
			}
			for (Floor f : glassfloors){
				for (Block b : bukkitElevator.baseBlocks){
					Block gb = event.getBlock().getWorld().getBlockAt(b.getX(), f.getY()-2, b.getZ());
					gb.setType(Material.AIR);
					bukkitElevator.glassBlocks.add(gb);
				}
			}
			//Apply impulse to players
			for (Entity p : bukkitElevator.passengers){
				if (p instanceof Player){
					((Player) p).setAllowFlight(true);
					if (plugin.useAntiCheat)
						AnticheatAPI.exemptPlayer((Player) p, CheckType.FLY);
				}
				
				if (plugin.useSpout){
					if (p instanceof Player){
						SpoutManager.getPlayer((Player) p).setGravityMultiplier(0);
						SpoutManager.getPlayer((Player) p).setCanFly(true);
					}						
				}
				if (bukkitElevator.destFloor.getY() > startFloor.getY()){
					bukkitElevator.goingUp = true;
				} else {
					BukkitElevatorManager.fallers.add(p);
				}
			}
			
			BukkitElevatorManager.bukkitElevators.add(bukkitElevator);

			if (BukkitLift.debug){
				System.out.println("Going Up: " + Boolean.toString(bukkitElevator.goingUp));
				System.out.println("Number of passengers: " + Integer.toString(bukkitElevator.passengers.size()));
				System.out.println("Elevator chunks: " + Integer.toString(bukkitElevator.chunks.size()));
				System.out.println("Total generation time: " + Long.toString(System.currentTimeMillis() - startTime));
			}
		}
		
	}
	
}