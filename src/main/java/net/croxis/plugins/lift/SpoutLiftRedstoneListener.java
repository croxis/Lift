package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.Iterator;

import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.material.Material;
import org.spout.api.material.block.BlockFace;

import org.spout.vanilla.component.living.Living;
import org.spout.vanilla.component.living.neutral.Human;
import org.spout.vanilla.component.substance.material.Sign;
import org.spout.vanilla.event.block.RedstoneChangeEvent;
import org.spout.vanilla.material.VanillaMaterials;

public class SpoutLiftRedstoneListener implements Listener{
	private final SpoutLift plugin;
	public SpoutLiftRedstoneListener(SpoutLift plugin){
		plugin.getEngine().getEventManager().registerEvents(this, plugin);
		this.plugin = plugin;
	} 
	
	@EventHandler
	public void onBlockRedstoneChange(RedstoneChangeEvent event){
		SpoutElevator elevator = null;
		if ((event.getBlock().getMaterial() == Material.get("STONE_BUTTON") || event.getBlock().getMaterial() == Material.get("WOOD_BUTTON")) 
				//&& (!event.getBlock().isBlockIndirectlyPowered())
				&& event.getBlock().translate(BlockFace.TOP).getMaterial() == Material.get("WALL_SIGN")){
			long startTime = System.currentTimeMillis();
			//plugin.logDebug("Initial reqs met");
			//elevator = new Elevator(this.plugin, event.getBlock());
			elevator = SpoutElevatorManager.createLift(event.getBlock());
			if (elevator == null)
				return;
			
			//See if lift is in use
			for (SpoutElevator e : SpoutElevatorManager.elevators){
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
			String line = ((Sign) event.getBlock().translate(BlockFace.TOP).getComponent()).getText()[2];
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
				plugin.logDebug("Number of entities in this chunk: " + Integer.toString(chunk.getEntities().size()));
				for(Entity e : chunk.getEntities()){
					if (e.has(Living.class)){
						if (elevator.isInShaftAtFloor(e, startFloor)){
							if (SpoutElevatorManager.isPassenger(e)){
								if (e instanceof Player)
									((Player) e).sendMessage("You are already in a lift. Relog in case this is an error.");
								continue;
							}
							elevator.addPassenger(e);
							if (iterator.hasNext() && plugin.autoPlace){
								e.getTransform().setPosition(iterator.next().getPosition().add(0.5, 0, 0.5));
							}
							if (e instanceof Player){
								Player player = (Player) e;
								if (e.get(Human.class).canFly()){
									SpoutElevatorManager.flyers.add(e);
									plugin.logDebug(player.getName() + " added to flying list");
								} else {
                                    ElevatorManager.flyers.remove(player);
                                    //player.setAllowFlight(false);
                                    plugin.logDebug(player.getName() + " NOT added to flying list");
                                }
								plugin.logDebug("Flyers: " + ElevatorManager.flyers.toString());
								if (!player.hasPermission("lift")){
									elevator.holders.put(e, e.getTransform().getPosition());
									elevator.passengers.remove(e);
								}
							}
						} else if (!elevator.isInShaftAtFloor(e, startFloor) && elevator.isInShaft(e)){
							elevator.holders.put(e, e.getTransform().getPosition());
							elevator.passengers.remove(e);
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
					Block gb = event.getBlock().getWorld().getBlock(b.getX(), f.getY()-2, b.getZ());
					gb.setMaterial(VanillaMaterials.AIR);
					elevator.glassBlocks.add(gb);
				}
			}
			//Apply impulse to players
			for (Entity p : elevator.getPassengers()){
				if (p instanceof Player){
					p.get(Human.class).setCanFly(true);
				}
				
				if (elevator.destFloor.getY() > startFloor.getY()){
					elevator.goingUp = true;
				} else {
					SpoutElevatorManager.fallers.add(p);
				}
			}
			
			SpoutElevatorManager.elevators.add(elevator);

			if (Lift.debug){
				System.out.println("Going Up: " + Boolean.toString(elevator.goingUp));
				System.out.println("Number of passengers: " + Integer.toString(elevator.passengers.size()));
				System.out.println("Elevator chunks: " + Integer.toString(elevator.chunks.size()));
				System.out.println("Total generation time: " + Long.toString(System.currentTimeMillis() - startTime));
			}
		}
		
	}

}
