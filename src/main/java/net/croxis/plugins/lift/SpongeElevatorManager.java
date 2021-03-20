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

import java.util.HashSet;
import java.util.Iterator;

import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.mutable.entity.FallDistanceData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

public class SpongeElevatorManager extends ElevatorManager{
    private static SpongeLift plugin;
    static HashSet<SpongeElevator> elevators = new HashSet<>();
    static HashSet<Entity> fallers = new HashSet<>();
    private static HashSet<Player> flyers = new HashSet<>();

    public SpongeElevatorManager(SpongeLift plugin) {
        SpongeElevatorManager.plugin = plugin;
	}
	
	static SpongeElevator createLift(Location<World> buttonLocation, String cause){
        long startTime = System.currentTimeMillis();
        String fullCause = "Starting elevator gen caused by: " + cause + " v" + plugin.toString();
		plugin.debug(fullCause);
		SpongeElevator elevator = new SpongeElevator(fullCause);
        int yscan = buttonLocation.getBlockY() - 1;
        while (yscan >= -1) {
            if (yscan == -1) {
                // Gone below the world. ABORT!
                plugin.debug("No elevator base found");
                elevator.setFailReason("No elevator base found");
                return null;
            }
            World world = buttonLocation.getExtent();
            Vector3i location = new Vector3i(buttonLocation.getBlockPosition().getX(), yscan, buttonLocation.getBlockPosition().getZ());
            Location<World> checkBlock = new Location<>(world, location);
            if (isValidShaftBlock(checkBlock.getBlock().getType())) {
                //Do nothing keep going
            } else if (isBaseBlock(checkBlock.getBlock().getType())) {
                elevator.baseBlockType = checkBlock.getBlockType();
                elevator.speed = plugin.getBlockSpeed(checkBlock.getBlockType());
                elevator.generateBaseBlocks(checkBlock);
                // Following is a speed optimization for entering a lift in use
                for (Location<World> l : elevator.baseBlocks) {
                    int chunkX = l.getBlockX() >> 4;
                    int chunkZ = l.getBlockZ() >> 4;
                    Chunk chunk = world.getChunk(chunkX, 0, chunkZ).get();
                    if (!elevator.chunks.contains(chunk))
                        elevator.chunks.add(chunk);
                }
                break;
            } else {
                // Something is obstructing the elevator, so stop
                plugin.debug("==Unknown Error Generating Lift==");
				plugin.debug("Yscan: " + Integer.toString(yscan));
				plugin.debug("Block: " + checkBlock.getBlock().getType().toString());
				plugin.debug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock.getBlockType())));
				plugin.debug("Is Base Block: " + Boolean.toString(SpongeElevatorManager.isBaseBlock(checkBlock.getBlockType())));
                return null;
            }
            yscan--;
        }
        plugin.debug("Base size: " + Integer.toString(elevator.baseBlocks.size()) + " at " + elevator.baseBlocks.iterator().next().getPosition().toString());

        elevator.constructFloors();

        //Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
        plugin.debug("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
		return elevator;
	}
	
	//Checks if block is a valid elevator block SANS iron
	static boolean isValidShaftBlock(BlockType blockType){
		return (SpongeConfig.floorMaterials.contains(blockType)
				|| blockType.equals(BlockTypes.AIR)
				|| blockType.equals(BlockTypes.LADDER)
				|| blockType.equals(BlockTypes.SNOW)
				|| blockType.equals(BlockTypes.FLOWING_WATER)
				|| blockType.equals(BlockTypes.STONE_BUTTON)
				|| blockType.equals(BlockTypes.TORCH)
				|| blockType.equals(BlockTypes.VINE)
				|| blockType.equals(BlockTypes.WALL_SIGN)
				|| blockType.equals(BlockTypes.WATER)
				|| blockType.equals(BlockTypes.WOODEN_BUTTON)
				|| blockType.equals(BlockTypes.CARPET)
				|| blockType.equals(BlockTypes.RAIL)
				|| blockType.equals(BlockTypes.DETECTOR_RAIL)
				|| blockType.equals(BlockTypes.ACTIVATOR_RAIL)
				|| blockType.equals(BlockTypes.GOLDEN_RAIL)
				|| blockType.equals(BlockTypes.REDSTONE_WIRE)
                || blockType.equals(BlockTypes.SNOW_LAYER));
	}
	
	//Recursive function that constructs our list of blocks
	//I'd rather it just return a hashset instead of passing elevator
	//But I can't figure out a clean way to do it
	static void scanBaseBlocks(Location<World> block, SpongeElevator elevator){
		if (elevator.baseBlocks.size() >= SpongeConfig.maxLiftArea || elevator.baseBlocks.contains(block))
			return; //5x5 max, prevents infinite loops
		elevator.baseBlocks.add(block);
		if (block.getRelative(Direction.NORTH).getBlockType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.NORTH), elevator);
		if (block.getRelative(Direction.EAST).getBlockType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.EAST), elevator);
		if (block.getRelative(Direction.SOUTH).getBlockType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.SOUTH), elevator);
		if (block.getRelative(Direction.WEST).getBlockType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.WEST), elevator);
	}
	
	public static void removePlayer(Player player, Iterator<Entity> passengers){
		for (SpongeElevator elevator : elevators){
			if (elevator.isInLift(player)){
				plugin.debug("Removing player from lift");
				restorePlayer(player);
				passengers.remove();
			}
		}
	}
	
	public static void removePlayer(Player player){
		for (SpongeElevator elevator : elevators){
			plugin.debug("Scanning lift");
			if (elevator.isInLift(player)){
				plugin.debug("Removing player from lift");
				player.setVelocity(new Vector3d(0, 0, 0));
				restorePlayer(player);
				elevator.removePassenger(player);
			}
		}
	}
	
	public static void removePassenger(Entity passenger){
		if (isPassenger(passenger)){
			passenger.setVelocity(new Vector3d(0, 0, 0));
			if (passenger instanceof Player)
				removePlayer((Player) passenger);
			else
				for (SpongeElevator elevator : elevators){
					plugin.debug("Scanning lift");
					if (elevator.isInLift(passenger))
						elevator.removePassenger(passenger);
				}
		}
	}
	
	public static boolean isBaseBlock(BlockType blockType){
		return SpongeConfig.blockSpeeds.containsKey(blockType);
	}
	
	public static boolean isPassenger(Entity entity){
		for (SpongeElevator elevator : elevators) {
			if (elevator.isInLift(entity))
				return true;
		}
		return false;
	}
	
	public static void setupPlayer(Player player){
		// Needed with modern servers?
		// Function which sets up a player for holding or passengering. Anti cheat stuff
		//if (player.getAllowFlight()){
		//	SpongeElevatorManager.fliers.add(player);
		//	plugin.debug(player.getName() + " added to flying list");
		//} else {
        //    SpongeElevatorManager.fliers.remove(player);
        //    //player.setAllowFlight(false);
		//	plugin.debug(player.getName() + " NOT added to flying list");
        //}

		//player.offer(Keys.CAN_FLY, true);
		
		//if (plugin.useNoCheatPlus)
		//	NCPExemptionManager.isExempted(player, fr.neatmonster.nocheatplus.checks.CheckType.FIGHT);
	}
	
	public static void restorePlayer(Player player){
		// Restores a player's previous stats.
		if (fallers.contains(player)){
			fallers.remove(player);
		}
		if (flyers.contains(player)){
			flyers.remove(player);
		} else {
			//player.offer(Keys.CAN_FLY, false);
            plugin.debug("Removing player from flight");
			//if (plugin.useNoCheatPlus)
			//	NCPExemptionManager.unexempt(player, fr.neatmonster.nocheatplus.checks.CheckType.FIGHT);
		}
	}
	
    public void run() {
		//Using while loop iterator so we can remove lifts in a sane way
		Iterator<SpongeElevator> eleviterator = elevators.iterator();
		// Various variables to reduce variable spawning
		SpongeElevator e;
		Iterator<Entity> passengers;
		Entity passenger;
		Entity holder;
		
		while (eleviterator.hasNext()){
			e = eleviterator.next();
            plugin.debug("Processing elevator: " + e);
			passengers = e.getPassengers();
			if(!passengers.hasNext()){
				e.endLift();
				eleviterator.remove();
				continue;
			}
			while (passengers.hasNext()){
				passenger = passengers.next();
				
				//Check if passengers have left the shaft
				if (!e.isInShaft(passenger)){
                    plugin.debug("Player out of shaft");
					if (Config.preventLeave){
						if (passenger instanceof Player)
							((Player) passenger).sendMessage(Text.of(Config.cantLeave));
                        Vector3d baseLoc = e.baseBlocks.iterator().next().getPosition();
						Location<World> playerLoc = passenger.getLocation();
						Vector3d newPosition = new Vector3d(baseLoc.getX() + 0.5D, playerLoc.getY(), baseLoc.getZ() + 0.5D);
						playerLoc.setPosition(newPosition);
						passenger.setLocationSafely(playerLoc);
					} else {
						passenger.setVelocity(new Vector3d(0, 0, 0));
						if (passenger instanceof Player)
							removePlayer((Player) passenger, passengers);
						else
							removePassenger(passenger);
						continue;
					}
				}
				
				//Re apply impulse as it does seem to run out
				if(e.destFloor.getFloor() > e.startFloor.getFloor())
					passenger.setVelocity(new Vector3d(0.0D, e.speed, 0.0D));
				else
					passenger.setVelocity(new Vector3d(0.0D, -e.speed, 0.0D));
                FallDistanceData fallData = passenger.get(FallDistanceData.class).get();
				passenger.offer(fallData.fallDistance().set(0.0F));
				
				if((e.goingUp && passenger.getLocation().getY() > e.destFloor.getY() - 0.7)
						|| (!e.goingUp && passenger.getLocation().getY() < e.destFloor.getY()-0.1)){
                    plugin.debug("Removing passenger: " + passenger.toString() + " with y " + Double.toString(passenger.getLocation().getY()));
                    plugin.debug("Trigger status: Going " + (e.goingUp ? "up" : "down"));
                    plugin.debug("Floor Y: " + Double.toString(e.destFloor.getY()));
					passenger.setVelocity(new Vector3d(0, 0, 0));
					Vector3d pLoc = passenger.getLocation().getPosition();
					Vector3d newLoc = new Vector3d(pLoc.getX(), e.destFloor.getY()-0.5, pLoc.getZ());
					passenger.getLocation().setPosition(newLoc);
					
					moveToHolder(e, passengers, passenger, passenger.getLocation());
				}
			}
			
			Iterator<Entity> holders = e.getHolders();
			
			while (holders.hasNext()){
				holder = holders.next();
                plugin.debug("Holding: " + holder.toString() + " at " + e.getHolderPos(holder));
				freezeEntity(holder, e.getHolderPos(holder));
			}
		}
	}

	private void freezeEntity(Entity holder, Location location) {
		holder.setLocationSafely(location);
		FallDistanceData fallData = holder.get(FallDistanceData.class).get();
		holder.offer(fallData.fallDistance().set(0.0F));
		holder.setVelocity(new Vector3d(0,0,0));
	}

	private void moveToHolder(SpongeElevator e, Iterator<Entity> passengers,
			Entity passenger, Location location) {
		passengers.remove();
		e.addHolder(passenger, location);
		passenger.setVelocity(new Vector3d(0,0,0));
        FallDistanceData fallData = passenger.get(FallDistanceData.class).get();
        passenger.offer(fallData.fallDistance().set(0.0F));
	}
	
	public static void addHolder(SpongeElevator elevator, Entity holder, Location location){
		// Adds a new entity to lift to be held in position
		if (holder instanceof Player)
			setupPlayer((Player) holder);
		elevator.addHolder(holder, location);
		if (!elevator.goingUp) {
			SpongeElevatorManager.fallers.add(holder);
		}
	}
	
	public static void addPassenger(SpongeElevator elevator, Entity passenger){
		// Adds a new entity to lift to be held in position
		if (passenger instanceof Player)
			setupPlayer((Player) passenger);
		elevator.addPassenger(passenger);
		if (!elevator.goingUp) {
			SpongeElevatorManager.fallers.add(passenger);
		}
		plugin.debug("[Manager] Added passenger " + passenger.toString());
	}

	public static void endAllLifts(){
	    //TODO: at lift end make sure all passengers are at correct floor
        for (SpongeElevator elevator : elevators){
            elevator.endLift();
        }
    }

    public static void reset(){
	    endAllLifts();
	    fallers.clear();
	    flyers.clear();
	    elevators.clear();
    }
}
