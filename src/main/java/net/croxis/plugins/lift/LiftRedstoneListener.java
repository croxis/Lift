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


public class LiftRedstoneListener implements Listener {
	private final Lift plugin;
	// Supporting annoying out of date servers
	private boolean canDo = false;
	public LiftRedstoneListener(Lift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	} 
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		Elevator elevator = null;
		canDo = false;
		try {
			canDo = (event.getBlock().getType() == Material.STONE_BUTTON || event.getBlock().getType() == Material.WOOD_BUTTON) 
					&& (!event.getBlock().isBlockIndirectlyPowered())
					&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN;
		} catch (Exception e) {
			canDo = event.getBlock().getType() == Material.STONE_BUTTON 
					&& (!event.getBlock().isBlockIndirectlyPowered())
					&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN;
		}
		/*if ((event.getBlock().getType() == Material.STONE_BUTTON || event.getBlock().getType() == Material.WOOD_BUTTON) 
				&& (!event.getBlock().isBlockIndirectlyPowered())
				&& event.getBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN){*/
		if (canDo){
			long startTime = System.currentTimeMillis();
			//plugin.logDebug("Initial reqs met");
			//elevator = new Elevator(this.plugin, event.getBlock());
			elevator = ElevatorManager.createLift(event.getBlock());
			if (elevator == null)
				return;
			
			//See if lift is in use
			for (Elevator e : ElevatorManager.elevators){
				Iterator<Block> iterator = elevator.baseBlocks.iterator();
				while (iterator.hasNext()){
					if (e.baseBlocks.contains(iterator.next()))
						return;
				}
			}
			
			if (elevator.getTotalFloors() < 2)
				return;
			
			int y = event.getBlock().getY();
			Floor startFloor = elevator.getFloormap().get(y);
			elevator.startFloor = startFloor;
			String line = ((Sign) event.getBlock().getRelative(BlockFace.UP).getState()).getLine(2);
			if (line.isEmpty())
				return;
			String[] splits = line.split(": ");
			if (splits.length != 2)
				return;
			int destination = Integer.parseInt(splits[1]);	

			//Get all players in elevator shaft (at floor of button pusher if possible)
			//And set their gravity to 0
			elevator.destFloor = elevator.getFloorFromN(destination);
			
			if (plugin.debug){
				System.out.println("Elevator start floor:" + startFloor.getFloor());
				System.out.println("Elevator start floor y:" + startFloor.getY());
				System.out.println("Elevator destination floor:" + destination);
				System.out.println("Elevator destination y:" + elevator.destFloor.getY());
			}
			
			Iterator<Block> iterator = elevator.baseBlocks.iterator();
			for(Chunk chunk : elevator.chunks){
				plugin.logDebug("Number of entities in this chunk: " + Integer.toString(chunk.getEntities().length));
				for(Entity e : chunk.getEntities()){
					if (e instanceof LivingEntity){
						if (elevator.isInShaftAtFloor(e, startFloor)){
							if (ElevatorManager.isPassenger(e)){
								if (e instanceof Player)
									((Player) e).sendMessage("You are already in a lift. Relog in case this is an error.");
								continue;
							}
							elevator.addPassenger((LivingEntity) e);
							if (iterator.hasNext() && plugin.autoPlace){
								Location loc = iterator.next().getLocation();
								e.teleport(new Location(e.getWorld(), loc.getX() + 0.5D, e.getLocation().getY(), loc.getZ() + 0.5D, e.getLocation().getYaw(), e.getLocation().getPitch()));
							}
							if (e instanceof Player){
								Player player = (Player) e;
								if (player.getAllowFlight()){
									ElevatorManager.flyers.add(player);
									plugin.logDebug(player.getName() + " added to flying list");
								} else {
                                    ElevatorManager.flyers.remove(player);
                                    //player.setAllowFlight(false);
                                    plugin.logDebug(player.getName() + " NOT added to flying list");
                                }
								plugin.logDebug("Flyers: " + ElevatorManager.flyers.toString());
								if (!player.hasPermission("lift")){
									elevator.holders.put((LivingEntity) e, e.getLocation());
									elevator.passengers.remove((LivingEntity) e);
								}
							}
						} else if (!elevator.isInShaftAtFloor(e, startFloor) && elevator.isInShaft(e)){
							elevator.holders.put((LivingEntity) e, e.getLocation());
							elevator.passengers.remove((LivingEntity) e);
						}
					}
				}
			}
			
			//Disable all glass inbetween players and destination
			ArrayList<Floor> glassfloors = new ArrayList<Floor>();
			//Going up
			if (startFloor.getY() < elevator.destFloor.getY()){
				for(int i = startFloor.getFloor() + 1; i<= elevator.destFloor.getFloor(); i++){
					glassfloors.add(elevator.getFloormap2().get(i));
				}
			}
			//Going down
			else {
				for(int i = elevator.destFloor.getFloor() + 1; i<= startFloor.getFloor(); i++){
					glassfloors.add(elevator.getFloormap2().get(i));
				}
			}
			for (Floor f : glassfloors){
				for (Block b : elevator.baseBlocks){
					Block gb = event.getBlock().getWorld().getBlockAt(b.getX(), f.getY()-2, b.getZ());
					gb.setType(Material.AIR);
					elevator.glassBlocks.add(gb);
				}
			}
			//Apply impulse to players
			for (Entity p : elevator.getPassengers()){
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
				if (elevator.destFloor.getY() > startFloor.getY()){
					elevator.goingUp = true;
				} else {
					ElevatorManager.fallers.add(p);
				}
			}
			
			ElevatorManager.elevators.add(elevator);

			if (Lift.debug){
				System.out.println("Going Up: " + Boolean.toString(elevator.goingUp));
				System.out.println("Number of passengers: " + Integer.toString(elevator.passengers.size()));
				System.out.println("Elevator chunks: " + Integer.toString(elevator.chunks.size()));
				System.out.println("Total generation time: " + Long.toString(System.currentTimeMillis() - startTime));
			}
		}
		
	}
	
}