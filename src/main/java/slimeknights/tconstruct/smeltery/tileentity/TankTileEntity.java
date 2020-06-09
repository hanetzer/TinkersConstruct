package slimeknights.tconstruct.smeltery.tileentity;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TankTileEntity extends SmelteryComponentTileEntity implements IFluidTankUpdater, FluidUpdatePacket.IFluidPacketReceiver {

  public static final int CAPACITY = FluidAttributes.BUCKET_VOLUME * 4;

  protected FluidTankAnimated tank;

  private final LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);
  private int lastStrength;

  public TankTileEntity() {
    this(TinkerSmeltery.tank.get());
    this.tank = new FluidTankAnimated(CAPACITY, this);
    this.lastStrength = -1;
  }

  public TankTileEntity(TileEntityType<?> tileEntityTypein) {
    super(tileEntityTypein);
  }

  @Override
  @Nonnull
  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      return holder.cast();
    return super.getCapability(capability, facing);
  }

  @Override
  public void read(CompoundNBT tag) {
    CompoundNBT fluid = tag.getCompound("tank");
    tank.readFromNBT(fluid);
    super.read(tag);
  }

  public void readTank(CompoundNBT nbt) {
    tank.readFromNBT(nbt);
  }

  @Nonnull
  @Override
  public CompoundNBT write(CompoundNBT tag) {
    CompoundNBT fluid = new CompoundNBT();
    tank.writeToNBT(fluid);
    tag.put("tank", fluid);
    return super.write(tag);
  }

  public void writeTank(CompoundNBT nbt) {
    tank.writeToNBT(nbt);
  }

  @Override
  public void onTankContentsChanged() {
  }

  public FluidTankAnimated getInternalTank() {
    return tank;
  }

  @Override
  public void updateFluidTo(FluidStack fluid) {
    int oldAmount = tank.getFluidAmount();
    tank.setFluid(fluid);

    tank.renderOffset += tank.getFluidAmount() - oldAmount;
  }
}
