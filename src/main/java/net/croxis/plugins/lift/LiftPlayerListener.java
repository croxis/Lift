package net.croxis.plugins.lift;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LiftPlayerListener implements Listener{
	private Lift plugin;

	public LiftPlayerListener(Lift plugin){
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event){
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			final Block signBlock = event.getClickedBlock();
			final Block buttonBlock = signBlock.getRelative(BlockFace.DOWN);

			if (signBlock.getType() == Material.WALL_SIGN
                && buttonBlock != null
			    && (buttonBlock.getType() == Material.STONE_BUTTON || buttonBlock.getType() == Material.WOOD_BUTTON)) {

				Sign sign = (Sign) signBlock.getState();
				Elevator elevator = ElevatorManager.createLift(buttonBlock);
                //Elevator elevator = new Elevator(this.plugin, buttonBlock);
				
				if (elevator == null){
					plugin.logInfo("Elevator generation returned a null object. Please report circumstances that generated this error.");
					return;
				}
				
				if (elevator.getTotalFloors() < 1){
					// This is just a button and sign, not an elevator.
					return;
				} else if (elevator.getTotalFloors() == 1){
					event.getPlayer().sendMessage(Lift.stringOneFloor);
					return;
				}
				
				event.setCancelled(true);
				
				int currentDestinationInt = 1;
				Floor currentFloor = elevator.getFloorFromY(buttonBlock.getY());
				
				String sign0 = Lift.stringCurrentFloor;
				String sign1 = Integer.toString(currentFloor.getFloor());
				String sign2 = "";
				String sign3 = "";
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
				// The following line MAY be what causes a potential bug for max floors
				if (currentDestinationInt > elevator.getTotalFloors()){
					currentDestinationInt = 1;
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					if (plugin.debug){
						System.out.println("Rotating back to first floor");
					}
				}
				sign2 = Lift.stringDestination + " " + Integer.toString(currentDestinationInt);
				sign3 = elevator.getFloorFromN(currentDestinationInt).getName();
				sign.setLine(0, sign0);
				sign.setLine(1, sign1);
				sign.setLine(2, sign2);
				sign.setLine(3, sign3);
				sign.update();
				plugin.logDebug("Completed sign update");
			}
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e){
		if(e.getCause() == DamageCause.FALL){
			Entity fallerE = e.getEntity();
			if (fallerE instanceof Player){
				Player faller = (Player) fallerE;
				if(ElevatorManager.fallers.contains(faller)){
					e.setCancelled(true);
					ElevatorManager.fallers.remove(faller);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		ElevatorManager.removePlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event){
		ElevatorManager.removePlayer(event.getPlayer());
	}

}
