package slimeknights.tconstruct.smeltery.tileentity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.tables.tileentity.TableTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CastingTileEntity extends TableTileEntity implements ITickableTileEntity, ISidedInventory {
  public FluidTankAnimated tank;
  public LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);
  protected int timer;
  public CastingTileEntity(TileEntityType<?> tileEntityTypeIn) {
    super(tileEntityTypeIn, "casting", 2, 1);
    this.tank = new FluidTankAnimated(0, this);
    this.itemHandler = new SidedInvWrapper(this, Direction.DOWN);
  }


  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      return holder.cast();
    return super.getCapability(capability, facing);
  }

  public void interact(PlayerEntity player) {
    // can't interact if liquid inside
    if (tank.getFluidAmount() > 0) {
      return;
    }

    // completely empty -> insert current item into input
    if (!isStackInSlot(0) && !isStackInSlot(1)) {
      ItemStack stack = player.inventory.decrStackSize(player.inventory.currentItem, stackSizeLimit);
      setInventorySlotContents(0, stack);
    }
    // take item out
    else {
      // take out stack 1 if something is there, 0 otherwise
      int slot = isStackInSlot(1) ? 1 : 0;

      // Additional info: Only 1 item can be put into the casting block usually, however recipes
      // can have ItemStacks with stacksize > 1 as output
      // we therefore spill the whole contents on extraction.
      ItemStack stack = getStackInSlot(slot);
      if (slot == 1) {
        // fire player smelt event?
      }
      ItemHandlerHelper.giveItemToPlayer(player, stack);
      setInventorySlotContents(slot, ItemStack.EMPTY);

      // send a block update for the comparator, needs to be done after the stack is removed
      if (slot == 1) {
        this.getWorld().notifyNeighborsOfStateChange(this.pos, this.getBlockState().getBlock());
      }
    }
  }


  @Override
  public int[] getSlotsForFace(Direction side) {
    return new int[]{0, 1};
  }

  @Override
  public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
    return index == 0 && !isStackInSlot(1);
  }

  @Override
  public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
    return index == 1;
  }

  @Override
  public void tick() {
    if (tank.getFluidAmount() == tank.getCapacity()) {
      timer++;
      if (!getWorld().isRemote) {
        if (timer >= 100) { // recipe.getTime()

        }
      }
    }
  }

  @Override
  public int getSizeInventory() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public ItemStack getStackInSlot(int index) {
    return null;
  }

  @Override
  public ItemStack decrStackSize(int index, int count) {
    return null;
  }

  @Override
  public ItemStack removeStackFromSlot(int index) {
    return null;
  }

  @Override
  public void setInventorySlotContents(int index, ItemStack stack) {

  }

  @Override
  public boolean isUsableByPlayer(PlayerEntity player) {
    return false;
  }

  @Override
  public void clear() {

  }

  public void updateFluidTo(FluidStack fluid) {
    int oldAmount = tank.getFluidAmount();
    tank.setFluid(fluid);

    if (fluid == null) {
      clear();
      return;
    }
    //tank.renderOffset += tank.getFluidAmount() - oldAmount;
  }

  @Override
  public CompoundNBT write(CompoundNBT tags) {
    tags = super.write(tags);
    CompoundNBT tankTag = new CompoundNBT();
    tank.writeToNBT(tankTag);
    tags.put("tank", tankTag);
    tags.putInt("timer", timer);
    return super.write(tags);
  }

  @Override
  public void read(CompoundNBT tags) {
    super.read(tags);

    tank.readFromNBT(tags.getCompound("tank"));

    updateFluidTo(tank.getFluid());

    timer = tags.getInt("timer");
  }

  @Nullable
  @Override
  public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
    return null;
  }
}
