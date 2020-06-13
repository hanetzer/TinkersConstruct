package slimeknights.tconstruct.library.smeltery;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IFaucetDepth {

  /**
   * Gets the depth the liquid flows into the block. Generally should be efficient as this is called each tick as part of the TESR
   *
   * @param world World access
   * @param pos   Position of this block
   * @param state Current state of the block
   * @return A positive float denoting how many blocks the flow extends.
   * Generally less than one, default is 0 for blocks without this interface
   * Return values best work between 0-1 in 1/16 intervalls, -1 being at the bottom of the block underneath
   */
  float getFlowDepth(World world, BlockPos pos, BlockState state);
}
