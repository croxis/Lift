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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

public class BukkitElevator extends Elevator{
    Set<Block> baseBlocks = new HashSet<>();
    private final Set<Entity> passengers = new HashSet<>();
    private final Map<Entity, Location> holders = new HashMap<>();
    private final Map<Location, BlockState> floorBlocks = new HashMap<>();
    private final Map<Location, BlockState> aboveFloorBlocks = new HashMap<>();
    private final Map<Entity, Vector> minecartSpeeds = new HashMap<>();
    HashSet<Chunk> chunks = new HashSet<>();
    Material baseBlockType = Material.IRON_BLOCK;
    private final BukkitLift plugin;

    public BukkitElevator(BukkitLift plugin){
        this.plugin = plugin;
        this.id = RandomStringUtils.randomAlphanumeric(6);
    }

    public String toString() { return "BukkitElevator[" + this.id + "]";}

    public void clear(){
        super.clear();
        baseBlocks.clear();
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
            return y >= floor.getY() - 1 && y <= floor.getY();
        }
        return false;
    }

    void addPassenger(Entity entity){
        if (entity == null) {
            plugin.logWarn("Trying to add null as a passenger. Ignore it.");
            return;
        }
        passengers.add(entity);
    }

    void addHolder(Entity entity, Location location, String reason){
        if (entity == null) {
            plugin.logWarn("Trying to add null as a holder. Ignore it.");
        }
        plugin.logDebug("Freeze holder " + entity + " in Lift. Reason: " + reason);
        holders.put(entity, location);
    }

    /**
     * Quickly ends the lift by teleporting all entities to the correct floor height
     */
    void tpPassengersToDest() {
        for (Entity passenger : passengers) {
            Location destination = passenger.getLocation();
            destination.setY(destFloor.getY());
            passenger.teleport(destination, TeleportCause.UNKNOWN);
            passenger.setFallDistance(0);
            if (passenger instanceof Player) {
                passenger.sendMessage("§7Lift timeout - You have been teleported to destination.");
            }
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
        else
            holders.remove(passenger);
    }

    Set<Entity> getPassengers(){
        return passengers;
    }

    Set<Entity> getHolders(){
        return holders.keySet();
    }

    public Location getHolderPos(Entity entity){
        return holders.get(entity);
    }

    public int getSize(){
        return passengers.size() + holders.size();
    }

    public Map<Location, BlockState> getFloorBlocks(){
        return floorBlocks;
    }

    public void addFloorBlock(Block block){
        floorBlocks.put(block.getLocation(), block.getState());
    }

    public Map<Location, BlockState> getAboveFloorBlocks(){
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

    public Map<Entity, Vector> getMinecartSpeeds(){
        return minecartSpeeds;
    }

    public void addMinecartSpeed(Minecart minecart){
        minecartSpeeds.put(minecart, minecart.getVelocity());
    }

    public BukkitFloor getDestFloor() {
        return (BukkitFloor) destFloor;
    }
}


