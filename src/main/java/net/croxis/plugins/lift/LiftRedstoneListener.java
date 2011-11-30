package net.croxis.plugins.lift;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.util.Vector;
import org.bukkit.block.Sign;
//import org.getspout.spoutapi.SpoutManager;

public class LiftRedstoneListener  extends BlockListener {
	private final Lift plugin; 
	public LiftRedstoneListener(Lift instance){
		this.plugin = instance;
	} 
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		Block block = event.getBlock();
		Elevator elevator = null;
		if ((block.getType() == Material.STONE_BUTTON) && (!block.isBlockIndirectlyPowered())){
			long startTime = System.currentTimeMillis();
			elevator = new Elevator(this.plugin, block);
			
			if (elevator.getTotalFloors() < 2)
				return;
			
			int y = block.getY();
			Floor startFloor = elevator.getFloormap().get(y);
			elevator.startFloor = startFloor;
			//Sign is:
			//Current Floor:
			//5
			//Destination Floor:
			//10
			int destination = Integer.parseInt(((Sign) block.getRelative(BlockFace.UP).getState()).getLine(3));	
			
			for (Floor destFloor : elevator.getFloormap().values()){
				if (destFloor.getFloor() == destination){
					//Get all players in elevator shaft (at floor of button pusher if possible)
					//And set their gravity to 0
					elevator.destFloor = destFloor;
					
					if (plugin.debug){
						System.out.println("Elevator start floor:" + startFloor.getFloor());
						System.out.println("Elevator destination floor:" + destination);
						System.out.println("Elevator destination y:" + destination);
					}
					
					for (Player p : block.getWorld().getPlayers()){
						if (elevator.isInShaftAtFloor(p, startFloor)){
							elevator.addPassenger(p);
							//SpoutManager.getPlayer(p).setGravityMultiplier(0);
							if (plugin.debug)
								System.out.println("Adding as passenger: " + p.getName());
						}
					}
					//Disable all glass inbetween players and destination
					ArrayList<Floor> glassfloors = new ArrayList<Floor>();
					//Going up
					if (startFloor.getY() < destFloor.getY()){
						for(int i = startFloor.getFloor() + 1; i<= destFloor.getFloor(); i++){
							glassfloors.add(elevator.getFloormap2().get(i));
						}
					}
					//Going down
					else {
						for(int i = destFloor.getFloor() + 1; i<= startFloor.getFloor(); i++){
							glassfloors.add(elevator.getFloormap2().get(i));
						}
					}
					for (Floor f : glassfloors){
						for (Block b : elevator.getFloorBlocks()){
							Block gb = block.getWorld().getBlockAt(b.getX(), f.getY()-2, b.getZ());
							gb.setType(Material.AIR);
							elevator.glassBlocks.add(gb);
						}
					}
					//Apply impulse to players
					for (Player p : elevator.getPassengers()){
						if (destFloor.getY() > startFloor.getY())
							p.setVelocity(new Vector(0.0D, 0.35D, 0.0D));
						else{
							p.setVelocity(new Vector(0.0D, -0.35D, 0.0D));
							plugin.fallers.add(p);
						}
					}
					elevator.taskid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, elevator, 2, 2);
					
					//Identify when button pusher is at desination
					//Reenable all glass
					//Remove impulse
					//Restore gravity to normal
				}
			}
			long tt = System.currentTimeMillis() - startTime;
			if (plugin.debug)
				System.out.println("Total time: " + tt);
		}
		
	}
	
}