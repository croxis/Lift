package net.croxis.plugins.lift;

import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitFloor extends Floor{
	private World world;
	private Block button;

	public BukkitFloor(final Block b, final int y){
		super(y);
		button = b;
	}

	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}
	public Block getButton() {
		return button;
	}
	public void setButton(Block button) {
		this.button = button;
	}
}
