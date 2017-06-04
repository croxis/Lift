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

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.block.DirectionalData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;

public class SpongeElevator extends Elevator{
    PluginContainer pluginContainer = Sponge.getPluginManager().getPlugin("lift").get();
    SpongeLift plugin = (SpongeLift) pluginContainer.getInstance().get();

	HashSet<Location<World>> baseBlocks = new HashSet<Location<World>>();
	private TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	private HashSet<Entity> passengers = new HashSet<Entity>();
	private HashMap<Entity, Location> holders = new HashMap<>();
	private HashMap<Location, FloorBlock> floorBlocks = new HashMap<>();
	private HashMap<Location, BlockSnapshot> aboveFloorBlocks = new HashMap<>();
	private HashMap<Entity, Vector3d> minecartSpeeds = new HashMap<>();
	HashSet<Chunk> chunks = new HashSet<>();
	BlockType baseBlockType = BlockTypes.IRON_BLOCK;
	
	SpongeFloor destFloor = null;
	Floor startFloor = null;

	public SpongeElevator(String cause){
	    this.cause = cause;
    }
	
	class FloorBlock{
		BlockSnapshot snapshot;
		FloorBlock(final BlockSnapshot m){
			snapshot = m;
		}
	}
	
	public void clear(){
		super.clear();
		baseBlocks.clear();
		worldFloorMap.clear();
		passengers.clear();
		floorBlocks.clear();
		aboveFloorBlocks.clear();
		minecartSpeeds.clear();
		holders.clear();
	}

    /**
     * @param blockLoc
     *
     * Recursive function that generates the base block grid based on the initial block
     * passed as the parameter.
     */
    public void generateBaseBlocks(Location<World> blockLoc) {
        if (this.baseBlocks.size() >= Config.liftArea || this.baseBlocks.contains(blockLoc))
            return;
        this.baseBlocks.add(blockLoc);
        if (blockLoc.getRelative(Direction.NORTH).getBlockType() == this.baseBlockType)
            generateBaseBlocks(blockLoc.getRelative(Direction.NORTH));
        if (blockLoc.getRelative(Direction.EAST).getBlockType() == this.baseBlockType)
            generateBaseBlocks(blockLoc.getRelative(Direction.EAST));
        if (blockLoc.getRelative(Direction.SOUTH).getBlockType() == this.baseBlockType)
            generateBaseBlocks(blockLoc.getRelative(Direction.SOUTH));
        if (blockLoc.getRelative(Direction.WEST).getBlockType() == this.baseBlockType)
            generateBaseBlocks(blockLoc.getRelative(Direction.WEST));
	}
	
	public Floor getFloorFromY(int y){
		return (Floor) super.getFloorFromY(y);
	}
	
	public SpongeFloor getFloorFromN(int n){
		return (SpongeFloor) super.getFloorFromN(n);
	}
	
	public boolean isInShaft(Entity entity){
		for (Location<World> block : baseBlocks){
			if (entity.getLocation().getY() >= block.getY() - 1.0D &&
					entity.getLocation().getY() <= floormap2.get(floormap2.lastKey()).getY() + 3.0D &&
					entity.getLocation().getBlockX() == block.getX() &&
					entity.getLocation().getBlockZ() == block.getZ())
				return true;
		}
		return false;
	}
	
	public boolean isInShaftAtFloor(Entity entity, Floor floor){
		if (isInShaft(entity)){
			if (entity.getLocation().getY() >= floor.getY() - 1 && entity.getLocation().getY() <= floor.getY())
				return true;
		}
		return false;
	}
	
	public void addPassenger(Entity entity){
		passengers.add(entity);
	}
	
	public void addHolder(Entity entity, Location location){
		holders.put(entity, location);
	}
	
	public void setPassengers(ArrayList<Entity> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public boolean isInLift(Entity entity){
		return (passengers.contains(entity) || holders.containsKey(entity));
	}
	
	public void removePassenger(Entity passenger){
		// NOt thread safe in an interation!
		if (passengers.contains(passenger))
			passengers.remove(passenger);
		else if (holders.containsKey(passenger))
			holders.remove(passenger);
	}
	
	public Iterator<Entity> getPassengers(){
		passengers.removeAll(Collections.singleton(null));
		return passengers.iterator();
	}
	
	public Iterator<Entity> getHolders(){
		if (holders.containsKey(null))
			holders.remove(null);
		return holders.keySet().iterator();
	}
	
	public Location getHolderPos(Entity entity){
		return holders.get(entity);
	}
	
	public int getSize(){
		return passengers.size() + holders.size();
	}
	
	public HashMap<Location, FloorBlock> getFloorBlocks(){
		return floorBlocks;
	}
	
	void addFloorBlock(Location<World> location){
		floorBlocks.put(location, new FloorBlock(location.createSnapshot()));
	}
	
	public void addCarpetBlock(Location<World> block){
		aboveFloorBlocks.put(block, block.createSnapshot());
	}

	public void addRailBlock(Location<World> block){ aboveFloorBlocks.put(block, block.createSnapshot()); }
	
	public void addRedstoneBlock(Location<World> block){
		aboveFloorBlocks.put(block, block.createSnapshot());
	}
	
	HashMap<Entity, Vector3d> getMinecartSpeeds(){
		return minecartSpeeds;
	}
	
	public void addMinecartSpeed(Minecart minecart){ minecartSpeeds.put(minecart, minecart.getVelocity()); }

	String constructFloors() {
        String message = "";
        int y1 = baseBlocks.iterator().next().getBlockY();
        int maxY = y1 + SpongeConfig.maxHeight;
        for (Location<World> b : baseBlocks){
            int x = b.getBlockX();
            int z = b.getBlockZ();
            int y = b.getBlockY();
            int scanHeight = 0;

            while (y <= maxY){
                y++;
                scanHeight ++;
                Location<World> testLocation = b.getExtent().getLocation(x, y, z);
                BlockState testBlock = testLocation.getBlock();
                if (!SpongeElevatorManager.isValidShaftBlock(testBlock.getType())){
                    message += " | " + x + " " + y + " " + z + " of type "  + testBlock.getType().toString();
                    maxY = y1;

                    plugin.debug("Not valid shaft block" + x + " " + y + " " + z + " of type "  + testBlock.getType().toString());
                    break;
                }
                if (testBlock.getType() == BlockTypes.STONE_BUTTON || testBlock.getType() == BlockTypes.WOODEN_BUTTON) {
                    if (Config.checkFloor)
                        if (!scanFloorAtY(y-2))
                            break;
                    //SpongeFloor floor = new SpongeFloor(b.getExtent().getLocation(x, y, z));
                    Floor floor = new Floor(y);
                    if (testLocation.getRelative(Direction.DOWN).getBlockType() == BlockTypes.WALL_SIGN){
                        TileEntity entity = testLocation.getRelative(Direction.DOWN).getTileEntity().get();
                        Sign sign = (Sign) entity;
                        floor.setName(sign.getSignData().get(1).get().toPlain());
                    }
                    if (testLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.WALL_SIGN)
                        this.floormap.put(y, floor);
                    plugin.debug("Floor added at lift: " + testLocation.toString());
                    plugin.debug("Floor y: " + Integer.toString(y));
                }
            }
        }
        int floorNumber = 1;
        Iterator<Integer> floorIterator = this.floormap.keySet().iterator();
        while (floorIterator.hasNext()){
            if (floorIterator.next() >= maxY)
                floorIterator.remove();
        }
        for (Floor floor : this.floormap.values()){
            floor.setFloor(floorNumber);
            floormap2.put(floorNumber, floor);
            floorNumber++;
        }
        return message;
    }

    /**
     * @param y
     * @return
     *
     * Checks the floor block at a given world height of y. Returns true if it is a valid floor
     */
    boolean scanFloorAtY(int y){
	    for (Location<World> location : this.baseBlocks){
	        BlockState checkBlock = location.getExtent().getBlock(location.getBlockX(), y, location.getBlockZ());
	        plugin.debug("Scan floor block type: " + checkBlock.toString());
	        if (!SpongeConfig.floorMaterials.contains(checkBlock.getType())
                    && !SpongeConfig.blockSpeeds.keySet().contains(checkBlock.getType())
                    && !(checkBlock.getType() == BlockTypes.AIR)) {
                // Check for air as some servers want the floors to magic replace if brokered.
                plugin.debug("Invalid block type in lift shaft.");
                plugin.debug("Is valid flooring?: " + SpongeConfig.floorMaterials.contains(checkBlock.getType()));
                plugin.debug("Is base?: " + Boolean.toString(SpongeConfig.blockSpeeds.keySet().contains(checkBlock.getType())));
                plugin.debug("Is air?: " + Boolean.toString(checkBlock.getType() == BlockTypes.AIR));
                return false;
            }
        }
	    return true;
    }

    void endLift(){
        plugin.debug("Halting lift: " + this.toString());
        for (Location location : floorBlocks.keySet()){
        	location.restoreSnapshot(floorBlocks.get(location).snapshot, true, BlockChangeFlag.ALL, Cause.source(this).build());
        	if (location.getBlockType() == BlockTypes.AIR && !Config.checkFloor)
        	    location.setBlockType(SpongeConfig.floorMaterials.iterator().next(), Cause.source(this).build());
        }
        for (Location location : aboveFloorBlocks.keySet()){
            location.restoreSnapshot(aboveFloorBlocks.get(location), true, BlockChangeFlag.ALL, Cause.source(this).build());
        }

        for (Iterator<Entity> iter = passengers.iterator(); iter.hasNext(); ){
            Entity e = iter.next();
         	SpongeElevatorManager.fallers.remove(e);
         	e.setVelocity(new Vector3d(0, 0, 0));
         	if (e instanceof Player)
         	    SpongeElevatorManager.removePlayer((Player) e);
            else if (e instanceof Minecart)
                e.setVelocity(getMinecartSpeeds().get(e));
            iter.remove();
		}

        for (Iterator<Entity> iter = getHolders(); iter.hasNext(); ){
            Entity passenger = iter.next();
            if (passenger instanceof Player)
                SpongeElevatorManager.removePlayer((Player) passenger, iter);
            else if (passenger instanceof Minecart)
                passenger.setVelocity(getMinecartSpeeds().get(passenger));
        }

        //Fire off redstone signal for arrival
        // TileEntity entity = testLocation.getRelative(Direction.DOWN).getTileEntity().get();
        //Sign sign = (Sign) entity;
        Location<World> signLocation = destFloor.buttonLocation.getRelative(Direction.UP);
        Direction direction = signLocation.get(DirectionalData.class).get().direction().get();
        Direction behindBlock = Direction.NORTH;
        if (direction.equals(Direction.NORTH))
            behindBlock = Direction.SOUTH;
        else if (direction.equals(Direction.EAST))
            behindBlock = Direction.WEST;
        else if (direction.equals(Direction.SOUTH))
            behindBlock = Direction.NORTH;
        else if (direction.equals(Direction.WEST))
            behindBlock = Direction.EAST;

        Location<World> testBlock = signLocation.getRelative(behindBlock).getRelative(behindBlock);
        if (testBlock.getBlockType().equals(BlockTypes.STONE_BUTTON) || testBlock.getBlockType().equals(BlockTypes.WOODEN_BUTTON)){
            testBlock.offer(Keys.POWERED, true, Cause.source(this).build());
        }

        clear();
    }
}


