package net.croxis.plugins.lift;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

/**
 * Created by croxis on 5/29/17.
 */
public class SpongeMovePreventListener {
    @Listener
    public void onPlayerMove(MoveEntityEvent event){
        for (SpongeElevator elevator : SpongeElevatorManager.elevators){
            World world = event.getToTransform().getExtent();
            Chunk chunk = world.getChunkAtBlock(event.getToTransform().getLocation().getBlockPosition()).get();
            if (elevator.chunks.contains(chunk)){
                if (elevator.isInShaft(event.getTargetEntity())
                        && ! elevator.isInLift(event.getTargetEntity())
                        && SpongeConfig.preventEntry){
                    event.setToTransform(event.getFromTransform());
                    //event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(-1));
                    if (event.getTargetEntity() instanceof Player){
                        ((Player) event.getTargetEntity()).sendMessage(Text.of(SpongeConfig.stringCantEnter));
                    }
                }
            }
        }
    }
}
