package net.croxis.plugins.lift;

import org.spongepowered.api.block.BlockLoc;

public class SpongeFloor extends Floor{
	
	private BlockLoc button;

	public SpongeFloor(final BlockLoc b, final int y){
		super(y);
		button = b;
	}

}
