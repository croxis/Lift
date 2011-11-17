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
import org.getspout.spoutapi.SpoutManager;

public class LiftRedstoneListener  extends BlockListener {
	private final Lift plugin; 
	public LiftRedstoneListener(Lift instance){
		this.plugin = instance;
	} 
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		long totalTime = System.currentTimeMillis();
		if (plugin.debug)
			System.out.println("Starting elevator generation");
		
		Block block = event.getBlock();
		Elevator elevator = null;
		if ((block.getType() == Material.STONE_BUTTON) && (!block.isBlockIndirectlyPowered())){
			elevator = new Elevator(this.plugin, block);
			int y = block.getY();
			Floor startFloor = elevator.getFloormap().get(y);
			//Sign is:
			//Current Floor:
			//5
			//Destination Floor:
			//10
			int destination = Integer.parseInt(((Sign) block.getRelative(BlockFace.UP)).getLine(3));
			for (Floor destFloor : elevator.getFloormap().values()){
				if (destFloor.getFloor() == destination){
					//Get all players in elevator shaft (at floor of button pusher if possible)
					//And set their gravity to 0
					//ArrayList<Player> players = new ArrayList<Player>();
					for (Player p : block.getWorld().getPlayers()){
						if (elevator.isInShaftAtFloor(p, startFloor)){
							elevator.addPassenger(p);
							SpoutManager.getPlayer(p).setGravityMultiplier(0);
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
							p.setVelocity(new Vector(0.0D, 0.5D, 0.0D));
						else
							p.setVelocity(new Vector(0.0D, -0.5D, 0.0D));
					}
					elevator.taskid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, elevator, 10, 10);
					//Identify when button pusher is at desination
					//Reenable all glass
					//Remove impulse
					//Restore gravity to normal
				}
			}
			long tt = System.currentTimeMillis() - totalTime;
			if (plugin.debug)
				System.out.println("Total time: " + tt);
		}
		
	}
	
}