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

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.block.Sign;


public class BukkitLiftRedstoneListener implements Listener {
	private final BukkitLift plugin;
	BukkitElevator bukkitElevator = null;
	
	// Supporting annoying out of date servers
	private boolean canDo = false;
	private Block block = null;
	
	public BukkitLiftRedstoneListener(BukkitLift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	} 
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		block = event.getBlock();
		canDo = false;
		canDo = BukkitElevatorManager.isButton(event.getBlock())
				&& (!event.getBlock().isBlockIndirectlyPowered())
				&& event.getBlock().getRelative(BlockFace.UP).getType().toString().matches(".*?WALL_SIGN");
		String reason = "Button press";
		
		if (BukkitConfig.redstone){
			plugin.logDebug("Redstone scan of " + event.getBlock().toString());
			reason = "Redstone signal event";
			Block[] blocks = new Block[4];
			blocks[0] = event.getBlock().getRelative(BlockFace.EAST);
			blocks[1] = event.getBlock().getRelative(BlockFace.WEST);
			blocks[2] = event.getBlock().getRelative(BlockFace.NORTH);
			blocks[3] = event.getBlock().getRelative(BlockFace.SOUTH);
			
			for (Block b : blocks){
				Block[] blocks2 = new Block[4];
				blocks2[0] = b.getRelative(BlockFace.EAST);
				blocks2[1] = b.getRelative(BlockFace.WEST);
				blocks2[2] = b.getRelative(BlockFace.NORTH);
				blocks2[3] = b.getRelative(BlockFace.SOUTH);
				for (Block b2 : blocks2){
					if (BukkitElevatorManager.isButton(b2)
							&& BukkitConfig.signMaterials.contains(b2.getRelative(BlockFace.UP).getType())){
						canDo = true;
						block = b2;
						break;
					}
				}
			}
			plugin.logDebug("Redstone scan no match");
		}
		plugin.logDebug("CanDo: " + canDo);

		if (canDo){
            plugin.logDebug("Initializing elevator for run");
			long startTime = System.currentTimeMillis();
			bukkitElevator = BukkitElevatorManager.createLift(block, reason);
			if (bukkitElevator == null){
				plugin.logDebug("Redstone elevator generation returned a null object. Button block at: " + block.getLocation().toString());
				plugin.logDebug("Please see previous messages to determine why.");
				return;
			}

            if (bukkitElevator.getTotalFloors() < 2)
                return;

            Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
			LiftSign liftSign = new LiftSign(plugin.config, sign.getLine(0), sign.getLine(1), sign.getLine(2), sign.getLine(3));
			int destination = liftSign.getDestinationFloor();
			//See if lift is in use
			for (BukkitElevator e : BukkitElevatorManager.bukkitElevators){
				for (Block baseBlock : bukkitElevator.baseBlocks) {
					if (e.baseBlocks.contains(baseBlock))
						return;
				}
			}
			
			int y = block.getY();
			BukkitFloor startFloor = bukkitElevator.getFloorFromY(y);
			bukkitElevator.startFloor = startFloor;
			bukkitElevator.destFloor = bukkitElevator.getFloorFromN(destination);

            plugin.logDebug("Sign destination: " + destination);
            plugin.logDebug("Floormap: " + bukkitElevator.floormap.toString());
            plugin.logDebug("Floormap2: " + bukkitElevator.floormap2.toString());
            plugin.logDebug("Start y: " + y);
			
			if (startFloor == null || bukkitElevator.destFloor == null){
				plugin.logInfo("Critical Error. Startfloor||DestFloor is null. Please report entire stacktrace plus the following error codes.");
				return;
			}
			
			if (bukkitElevator.destFloor.getY() > startFloor.getY()){
				bukkitElevator.goingUp = true;
			}

			plugin.logDebug("Elevator start floor:" + startFloor.getFloor());
			plugin.logDebug("Elevator start floor y:" + startFloor.getY());
			plugin.logDebug("Elevator destination floor:" + destination);
			plugin.logDebug("Elevator destination y:" + bukkitElevator.destFloor.getY());
			
			Iterator<Block> baseBlocksIterator = bukkitElevator.baseBlocks.iterator();
			for(Chunk chunk : bukkitElevator.chunks){
				plugin.logDebug("Number of entities in this chunk: " + chunk.getEntities().length);
				for(Entity entity : chunk.getEntities()){
					if (!BukkitConfig.liftMobs && !(entity instanceof Player))
						continue;
					if (bukkitElevator.isInShaftAtFloor(entity, startFloor)){
						if (BukkitElevatorManager.isPassenger(entity)){
							if (entity instanceof Player)
								entity.sendMessage("You are already in a lift. Relog in case this is an error.");
							continue;
						}
						
						if (entity instanceof Minecart){
							bukkitElevator.addMinecartSpeed((Minecart) entity);
							//A minecart wont go up if attached to a rail, so we temp remove the rail.  
							if (bukkitElevator.goingUp
									&& (entity.getLocation().getBlock().getType() == Material.RAIL
									|| entity.getLocation().getBlock().getType() == Material.DETECTOR_RAIL
									|| entity.getLocation().getBlock().getType() == Material.ACTIVATOR_RAIL
									|| entity.getLocation().getBlock().getType() == Material.POWERED_RAIL)){
								new BukkitRestoreRailTask(entity.getLocation().getBlock()).runTaskLater(plugin, 10);
								entity.getLocation().getBlock().setType(Material.AIR);
							}
							plugin.logDebug("Minecart added to lift");
						}
						plugin.logDebug("Adding passenger " + entity.toString());
						BukkitElevatorManager.addPassenger(bukkitElevator, entity);
						plugin.logDebug("Added passenger " + entity.toString());
						if (baseBlocksIterator.hasNext() && BukkitConfig.autoPlace){
							Location loc = baseBlocksIterator.next().getLocation();
							entity.teleport(new Location(entity.getWorld(), loc.getX() + 0.5D, entity.getLocation().getY(), loc.getZ() + 0.5D, entity.getLocation().getYaw(), entity.getLocation().getPitch()));
						}
						if (entity instanceof Player){
							Player player = (Player) entity;
							plugin.logDebug("Flyers: " + BukkitElevatorManager.fliers.toString());
							if (!player.hasPermission("lift")){
								BukkitElevatorManager.addHolder(bukkitElevator, entity, entity.getLocation(), "Does not have lift permission.");
							}
						}
					} else if (!bukkitElevator.isInShaftAtFloor(entity, startFloor)
							&& bukkitElevator.isInShaft(entity)
							&& !entity.isInsideVehicle()){
						BukkitElevatorManager.addHolder(bukkitElevator, entity, entity.getLocation(), "In active shaft but not at floor.");
					}
				}
			}
			
			//Disable all glass inbetween players and destination
			ArrayList<Floor> glassfloors = new ArrayList<Floor>();
			//Going up
			if (bukkitElevator.goingUp){
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
					bukkitElevator.addFloorBlock(gb);
					
					if (gb.getRelative(BlockFace.UP).getType().toString().contains("CARPET")){
						bukkitElevator.addCarpetBlock(gb.getRelative(BlockFace.UP));
						gb.getRelative(BlockFace.UP).setType(Material.AIR);
					} else if (gb.getRelative(BlockFace.UP).getType() == Material.RAIL
							|| gb.getRelative(BlockFace.UP).getType() == Material.DETECTOR_RAIL
							|| gb.getRelative(BlockFace.UP).getType() == Material.POWERED_RAIL
							|| gb.getRelative(BlockFace.UP).getType() == Material.ACTIVATOR_RAIL){
						bukkitElevator.addRailBlock(gb.getRelative(BlockFace.UP));
						gb.getRelative(BlockFace.UP).setType(Material.AIR);
					} else if (gb.getRelative(BlockFace.UP).getType() == Material.REDSTONE_WIRE) {
						bukkitElevator.addRedstoneBlock(gb.getRelative(BlockFace.UP));
						gb.getRelative(BlockFace.UP).setType(Material.AIR);
					}
					
					gb.setType(Material.AIR);
				}
			}
			
			BukkitElevatorManager.bukkitElevators.add(bukkitElevator);

			plugin.logDebug("Going Up: " + bukkitElevator.goingUp);
			plugin.logDebug("Number of passengers: " + bukkitElevator.getSize());
			plugin.logDebug("Elevator chunks: " + bukkitElevator.chunks.size());
			plugin.logDebug("Total generation time: " + (System.currentTimeMillis() - startTime));
		}
	}
}