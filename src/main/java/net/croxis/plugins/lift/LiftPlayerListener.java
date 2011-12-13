package net.croxis.plugins.lift;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class LiftPlayerListener extends PlayerListener{
	private Lift plugin;

	public LiftPlayerListener(Lift plugin){
		this.plugin = plugin;
	}
	
	public void onPlayerInteract(PlayerInteractEvent event){
		Elevator elevator = null;
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			if (event.getClickedBlock().getType().equals(Material.WALL_SIGN) 
					&& event.getClickedBlock().getRelative(BlockFace.DOWN).getType().equals(Material.STONE_BUTTON)){
				Sign sign = (Sign) event.getClickedBlock().getState();
				
				elevator = new Elevator(this.plugin, event.getClickedBlock().getRelative(BlockFace.DOWN));
				
				if (elevator.getTotalFloors() < 2){
					event.getPlayer().sendMessage("There is only one floor silly.");
					return;
				}
				
				int currentDestinationInt = 1;
				Floor currentFloor = elevator.getFloorFromY(event.getClickedBlock().getRelative(BlockFace.DOWN).getY());
				
				String sign0 = "Current Floor:";
				String sign1 = Integer.toString(currentFloor.getFloor());
				String sign2 = "";
				String sign3 = "";
				
				//if(sign.getLine(2).isEmpty()){
				//	currentDestinationInt = 0;
				//} 
				//If the current line isn't valid number
				try{
					String[] splits = sign.getLine(2).split(": ");
					currentDestinationInt = Integer.parseInt(splits[1]);	
				} catch (Exception e){
					currentDestinationInt = 0;
					if (plugin.debug){
						System.out.println("non Valid previous destination");
					}
				}
				currentDestinationInt++;
				if (currentDestinationInt == currentFloor.getFloor()){
					currentDestinationInt++;
					if (plugin.debug){
						System.out.println("Skipping current floor");
					}
				}
				// The following line MAY be what causes a potetal bug for max floors
				if (currentDestinationInt > elevator.getTotalFloors()){
					currentDestinationInt = 1;
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					if (plugin.debug){
						System.out.println("Rotating back to first floor");
					}
				}
				//sign2 = "Dest: " + elevator.getFloorFromN(currentDestinationInt).getName();
				//sign3 = Integer.toString(currentDestinationInt);
				sign2 = "Dest: " + Integer.toString(currentDestinationInt);
				sign3 = elevator.getFloorFromN(currentDestinationInt).getName();
				sign.setLine(0, sign0);
				sign.setLine(1, sign1);
				sign.setLine(2, sign2);
				sign.setLine(3, sign3);
				sign.update();
				if (plugin.debug){
					System.out.println("Completed sign update");
				}
			}
	}

}
