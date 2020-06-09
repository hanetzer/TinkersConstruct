package slimeknights.tconstruct.smeltery.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;

import static slimeknights.tconstruct.smeltery.block.FaucetBlock.FACING;

public class FaucetTileEntity extends TileEntity implements ITickableTileEntity {
  public Direction direction;
  public boolean isPouring;
  public boolean stopPouring;
  public FluidStack drained;
  public boolean lastRedstoneState;

  public FaucetTileEntity() {
    this(TinkerSmeltery.faucet.get());
  }
  public FaucetTileEntity(TileEntityType<?> tileEntityTypeIn) {
    super(tileEntityTypeIn);
    reset();
  }

  public void activate() {
    BlockState state = getWorld().getBlockState(pos);
    if (!state.getProperties().contains(FACING)) {
      return;
    }

    // already pouring? we want to stop then
    if (isPouring) {
      stopPouring = true;
      return;
    }

    direction = getWorld().getBlockState(pos).get(FACING);
    doTransfer();
    return;
  }

  public void handleRedstone(boolean hasSignal) {
    if (hasSignal != lastRedstoneState) {
      lastRedstoneState = hasSignal;
      if (hasSignal) {
        getWorld().getPendingBlockTicks().scheduleTick(pos, this.getBlockState().getBlock(), 2);
      }
    }
  }

  @Override
  public void tick() {
    if (getWorld().isRemote) {
      return;
    }

    // nothing to do if not pouring
    if (!isPouring) {
      return;
    }

    if (drained != null) {
      // done draining
      if (drained.getAmount() <= 0) {
        drained = null;
        // pour me another, if we want to
        if (!stopPouring) {
          doTransfer();
        }
        else {
          reset();
        }
      }
      else {
        // reduce amount (cooldown)
        pour();
      }
    }
  }

  protected void doTransfer() {
    // still got content left
    IFluidHandler toDrain = getFluidHandler(pos.offset(direction), direction.getOpposite());
    IFluidHandler toFill = getFluidHandler(pos.down(), Direction.UP);
    if (toDrain != null && toFill != null) {
      System.out.println("toDrain != null && toFill != null");
      // can we drain?
      FluidStack drained = toDrain.drain(144, IFluidHandler.FluidAction.SIMULATE);
      if (drained != null) {
        System.out.println("drained != null");
        // can we fill
        int filled = toFill.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled > 0) {
          System.out.println(String.format("filled=%d", filled));
          // drain the liquid and transfer it, buffer the amount for delay
          this.drained = toDrain.drain(filled, IFluidHandler.FluidAction.EXECUTE);
          this.isPouring = true;
          pour();

          // sync to clients
          if (!getWorld().isRemote()) { // TODO: getWorld() instanceof WorldServer
            // TODO: TinkerNetwork.sendToClients((WorldServer) getWorld(), pos, new FaucetActivationPacket(pos, drained));
          }

          return;
        }
      }
    }
    // draining unsuccessful
    reset();
  }

  protected void pour() {
    if (drained == null) {
      return;
    }

    IFluidHandler toFill = getFluidHandler(pos.down(), Direction.UP);
    if (toFill != null) {
      System.out.println("toFill != null");
      FluidStack fillStack = drained.copy();
      fillStack.setAmount(Math.min(drained.getAmount(), 6));

      // can we fill?
      int filled = toFill.fill(fillStack, IFluidHandler.FluidAction.SIMULATE);
      if (filled > 0) {
        System.out.println(String.format("filled=%d", filled));
        // transfer it
        this.drained.shrink(filled);
        fillStack.setAmount(filled);
        toFill.fill(fillStack, IFluidHandler.FluidAction.EXECUTE);
      }
    }
    else {
      // filling TE got lost. reset. all liquid buffered is lost.
      reset();
    }
  }

  protected void reset() {
    isPouring = false;
    stopPouring = false;
    drained = null;
    direction = Direction.DOWN;
    lastRedstoneState = false;
  }

  protected IFluidHandler getFluidHandler(BlockPos pos, Direction direction) {
    TileEntity te = getWorld().getTileEntity(pos);
    if (te != null) {
      return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction).orElse(null);
    }
    return null;
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
  }

  @Override
  public CompoundNBT write(CompoundNBT compound) {
    compound = super.write(compound);
    if (drained != null) {
      drained.writeToNBT(compound);
      compound.putInt("direction", direction.getIndex());
    }
    return compound;
  }

  @Override
  public void read(CompoundNBT compound) {
    super.read(compound);
    drained = FluidStack.loadFluidStackFromNBT(compound);

    if (drained != null) {
      isPouring = true;
      direction = Direction.values()[compound.getInt("direction")];
      stopPouring = compound.getBoolean("stop");
    }
    else {
      reset();
    }
  }

  public void onActivationPacket(FluidStack fluid) {
    if (fluid == null) {
      reset();
    }
    else {
      drained = fluid;
      isPouring = true;
      direction = getWorld().getBlockState(pos).get(FACING);
    }
  }

  @Nullable
  @Override
  public SUpdateTileEntityPacket getUpdatePacket() {
    CompoundNBT compound = new CompoundNBT();
    write(compound);
    return new SUpdateTileEntityPacket(this.getPos(), -1, compound);
  }

  @Override
  public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
    super.onDataPacket(net, pkt);
    read(pkt.getNbtCompound());
  }

  @Override
  public CompoundNBT getUpdateTag() {
    // new tag instead of super since default implementation calls the super of writeToNBT
    return write(new CompoundNBT());
  }

  @Override
  public void handleUpdateTag(CompoundNBT tag) {
    read(tag);
  }
}
