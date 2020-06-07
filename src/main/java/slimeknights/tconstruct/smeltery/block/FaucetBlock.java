package slimeknights.tconstruct.smeltery.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import slimeknights.tconstruct.smeltery.tileentity.FaucetTileEntity;

import javax.annotation.Nullable;
import java.util.Random;

public class FaucetBlock extends ContainerBlock {
  public static final DirectionProperty FACING = DirectionProperty.create("facing", (direction) -> {
    return direction != Direction.DOWN;
  });
  protected static final VoxelShape UP_SHAPE = Block.makeCuboidShape(4.0D, 10.0D, 4.0D, 12.0D, 16.0D, 12.0D);
  protected static final VoxelShape NORTH_SHAPE = Block.makeCuboidShape(4.0D, 4.0D, 0.0D, 12.0D, 10.0D, 6.0D);
  protected static final VoxelShape SOUTH_SHAPE = Block.makeCuboidShape(4.0D, 4.0D, 10.0D, 12.0D, 10.0D, 16.D);
  protected static final VoxelShape EAST_SHAPE = Block.makeCuboidShape(10.D, 4.0D, 4.0D, 16.0D, 10.0D, 12.0D);
  protected static final VoxelShape WEST_SHAPE = Block.makeCuboidShape(0.0D, 4.0D, 4.0D, 6.0D, 10.0D, 12.0D);
  public FaucetBlock(Properties builder) {
    super(builder);
    this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH));
  }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
    if (player.isSneaking()) {
      return ActionResultType.PASS;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof FaucetTileEntity) {
      ((FaucetTileEntity) te).activate();
      return ActionResultType.SUCCESS;
    }
    return super.onBlockActivated(state, worldIn, pos, player, handIn, hit);
  }

  @Override
  public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
    return true;
  }

  @Override
  public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
    if (worldIn.isRemote()) {
      return;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof FaucetTileEntity) {
      ((FaucetTileEntity) te).handleRedstone(worldIn.isBlockPowered(pos));
    }
  }

  @Override
  public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof FaucetTileEntity) {
      ((FaucetTileEntity) te).activate();
    }
  }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
    switch (state.get(FACING)) {
      case UP:
        return UP_SHAPE;
      case NORTH:
        return NORTH_SHAPE;
      case SOUTH:
        return SOUTH_SHAPE;
      case EAST:
        return EAST_SHAPE;
      case WEST:
        return WEST_SHAPE;
      default:
        return VoxelShapes.fullCube();
    }
  }

  @Nullable
  public TileEntity createNewTileEntity(IBlockReader worldIn) {
    return new FaucetTileEntity();
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockItemUseContext context) {
    Direction dir = context.getFace().getOpposite();
    if (dir == Direction.DOWN) {
      dir = context.getPlacementHorizontalFacing().getOpposite();
    }
    return this.getDefaultState().with(FACING, dir);
    //return this.getDefaultState().with(FACING, dir.getAxis() == Direction.Axis.Y ? Direction.UP : dir);
  }

  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }
}
