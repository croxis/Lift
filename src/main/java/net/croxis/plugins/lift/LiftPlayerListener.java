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
				Sign sign = (Sign)event.getClickedBlock().getState();
				elevator = new Elevator(this.plugin, event.getClickedBlock().getRelative(BlockFace.DOWN));
				
				if (elevator.getTotalFloors() < 2){
					event.getPlayer().sendMessage("There is only one floor silly.");
					return;
				}
				
				int currentFloorInt;
				int currentDestinationInt = 1;
				Floor currentFloor = elevator.getFloorFromY(event.getClickedBlock().getRelative(BlockFace.DOWN).getY());
				String sign1 = "Current Floor:";
				String sign2 = Integer.toString(currentFloor.getFloor());
				String sign3;
				String sign4;
				
				if(sign.getLine(4).isEmpty())
					currentDestinationInt = 0;
				//If the current line isn't valid number
				try{
					currentDestinationInt = Integer.parseInt(sign.getLine(4));
				} catch (NumberFormatException e){
					currentDestinationInt = 0;
				}
				currentDestinationInt += 1;
				if (currentDestinationInt == currentFloor.getFloor())
					currentDestinationInt += 1;
				// The following line MAY be what causes a potetal bug for max floors
				if (currentDestinationInt > elevator.getTotalFloors())
					currentDestinationInt = 1;
				
				sign3 = "Dest: " + elevator.getFloorFromN(currentDestinationInt).getName();
				sign4 = Integer.toString(currentDestinationInt);
				sign.setLine(1, sign1);
				sign.setLine(2, sign2);
				sign.setLine(3, sign3);
				sign.setLine(4, sign4);
			}
	}

}
