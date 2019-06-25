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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class BukkitElevator extends Elevator{
    HashSet<Block> baseBlocks = new HashSet<Block>();
    //private TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
    private HashSet<Entity> passengers = new HashSet<>();
    private HashMap<Entity, Location> holders = new HashMap<>();
    private HashMap<Location, BlockState> floorBlocks = new HashMap<>();
    private HashMap<Location, BlockState> aboveFloorBlocks = new HashMap<>();
    private HashMap<Entity, Vector> minecartSpeeds = new HashMap<>();
    HashSet<Chunk> chunks = new HashSet<>();
    Material baseBlockType = Material.IRON_BLOCK;
    private BukkitLift plugin;

    public BukkitElevator(BukkitLift plugin){
        this.plugin = plugin;
        this.id = RandomStringUtils.randomAlphanumeric(6);
    }

    public String toString() { return "BukkitElevator[" + this.id.toString() + "]";}

    public void clear(){
        super.clear();
        baseBlocks.clear();
        //worldFloorMap.clear();
        passengers.clear();
        floorBlocks.clear();
        aboveFloorBlocks.clear();
        minecartSpeeds.clear();
        holders.clear();
    }

    public BukkitFloor getFloorFromY(int y){
        return (BukkitFloor) super.getFloorFromY(y);
    }

    public BukkitFloor getFloorFromN(int n){
        return (BukkitFloor) super.getFloorFromN(n);
    }

    boolean isInShaft(Entity entity){
        for (Block block : baseBlocks){
            if (entity.getLocation().getY() >= block.getLocation().getY() - 1.0D &&
                    entity.getLocation().getY() <= floormap2.get(floormap2.lastKey()).getY() + 3.0D &&
                    entity.getLocation().getBlockX() == block.getX() &&
                    entity.getLocation().getBlockZ() == block.getZ())
                return true;
        }
        return false;
    }

    boolean isInShaftAtFloor(Entity entity, Floor floor){
        if (isInShaft(entity)){
            double y = entity.getLocation().getY();
            if (entity.isInsideVehicle())
                y -= entity.getVehicle().getHeight();
            if (y >= floor.getY() - 1 && y <= floor.getY())
                return true;
        }
        return false;
    }

    void addPassenger(Entity entity){
        passengers.add(entity);
    }

    void addHolder(Entity entity, Location location, String reason){
        plugin.logDebug("Holder " + entity + " added for: " + reason);
        holders.put(entity, location);
    }

    public void setPassengers(ArrayList<LivingEntity> entities){
        passengers.clear();
        passengers.addAll(entities);
    }

    /**
     * Quickly ends the lift by teleporting all entities to the correct floor height
     */
    void quickEndLift() {
        Iterator<Entity> passiterator = passengers.iterator();
        while (passiterator.hasNext()) {
            Entity passenger = passiterator.next();
            if (passenger == null) {
                passiterator.remove();
                continue;
            }
            Location destination = passenger.getLocation();
            destination.setY(destFloor.getY());
            passenger.teleport(destination);
            passenger.setFallDistance(0);
        }
    }

    boolean isInLift(Entity entity){
        return (passengers.contains(entity) || holders.containsKey(entity));
    }

    void removePassenger(Entity passenger){
        // Not thread safe in an interaction!
        passenger.setFallDistance(0);
        if (passengers.contains(passenger))
            passengers.remove(passenger);
        else if (holders.containsKey(passenger))
            holders.remove(passenger);
    }

    Iterator<Entity> getPassengers(){
        passengers.removeAll(Collections.singleton(null));
        return passengers.iterator();
    }

    Iterator<Entity> getHolders(){
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

    public HashMap<Location, BlockState> getFloorBlocks(){
        return floorBlocks;
    }

    public void addFloorBlock(Block block){
        floorBlocks.put(block.getLocation(), block.getState());
    }

    public HashMap<Location, BlockState> getAboveFloorBlocks(){
        return aboveFloorBlocks;
    }

    public void addCarpetBlock(Block block){
        aboveFloorBlocks.put(block.getLocation(), block.getState());
    }

    public void addRailBlock(Block block){
        aboveFloorBlocks.put(block.getLocation(), block.getState());
    }

    public void addRedstoneBlock(Block block){
        aboveFloorBlocks.put(block.getLocation(), block.getState());
    }

    public HashMap<Entity, Vector> getMinecartSpeeds(){
        return minecartSpeeds;
    }

    public void addMinecartSpeed(Minecart minecart){
        minecartSpeeds.put(minecart, minecart.getVelocity());
    }
}


