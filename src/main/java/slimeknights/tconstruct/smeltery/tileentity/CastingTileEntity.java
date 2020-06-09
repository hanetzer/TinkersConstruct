package slimeknights.tconstruct.smeltery.tileentity;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import slimeknights.mantle.tileentity.InventoryTileEntity;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.smeltery.CastingFluidHandler;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.recipe.CastingRecipe;
import slimeknights.tconstruct.tables.tileentity.TableTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CastingTileEntity extends InventoryTileEntity implements ITickableTileEntity, ISidedInventory {
  private int INPUT = 0;
  private int OUTPUT = 1;
  public FluidTankAnimated tank;
  public LazyOptional<CastingFluidHandler> holder = LazyOptional.of(() -> new CastingFluidHandler(this, tank));
  protected int timer;
  private List<CastingRecipe> recipes = Lists.newArrayList();
  protected CastingRecipe recipe;
  public CastingTileEntity(TileEntityType<?> tileEntityTypeIn) {
    super(tileEntityTypeIn, new TranslationTextComponent("casting"), 2, 1);
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
      System.out.println("can't interact if liquid inside");
      return;
    }

    // completely empty -> insert current item into input
    if (!isStackInSlot(0) && !isStackInSlot(1)) {
      System.out.println("completely empty -> insert current item into input");
      ItemStack stack = player.inventory.decrStackSize(player.inventory.currentItem, stackSizeLimit);
      setInventorySlotContents(INPUT, stack);
    }
    // take item out
    else {
      System.out.println("take item out");
      // take out stack 1 if something is there, 0 otherwise
      int slot = isStackInSlot(OUTPUT) ? OUTPUT : INPUT;

      // Additional info: Only 1 item can be put into the casting block usually, however recipes
      // can have ItemStacks with stacksize > 1 as output
      // we therefore spill the whole contents on extraction.
      ItemStack stack = getStackInSlot(slot);
      if (slot == OUTPUT) {
        // fire player smelt event?
        System.out.println("fire player smelt event?");
      }
      ItemHandlerHelper.giveItemToPlayer(player, stack);
      setInventorySlotContents(slot, ItemStack.EMPTY);

      // send a block update for the comparator, needs to be done after the stack is removed
      if (slot == OUTPUT) {
        System.out.println("send a block update");
        this.getWorld().notifyNeighborsOfStateChange(this.pos, this.getBlockState().getBlock());
      }
    }
  }


  @Override
  public int[] getSlotsForFace(Direction side) {
    return new int[]{INPUT, OUTPUT};
  }

  @Override
  public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
    return index == INPUT && !isStackInSlot(OUTPUT);
  }

  @Override
  public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
    return index == 1;
  }

  @Override
  public void tick() {
    // no recipe
    if (recipe == null) {
      return;
    }
    // fully filled
    if (tank.getFluidAmount() == tank.getCapacity()) {
      timer++;
      System.out.println(String.format("timer=%d", timer));
      if (!getWorld().isRemote) {
        if (timer >= recipe.getCoolingTime()) { // recipe.getTime()
          if (recipe.consumesCast()) {
            setInventorySlotContents(INPUT, ItemStack.EMPTY);
          }
          setInventorySlotContents(OUTPUT, recipe.getRecipeOutput());
          getWorld().playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.AMBIENT, 0.07f, 4f);

          reset();

          getWorld().notifyNeighborsOfStateChange(this.pos, this.getBlockState().getBlock());
        }
      }
    }
  }

  protected CastingRecipe findRecipe(Fluid fluid) {
    if (recipes.isEmpty()) {
      recipes = world.getRecipeManager().getRecipes(TinkerSmeltery.castingRecipeType, this, world);
      System.out.println(recipes);
    }
    for (CastingRecipe candidate : recipes) {
      System.out.println(candidate.toString());
      if (candidate.matches(fluid, this, world)) {
        recipe = candidate;
        System.out.println("candidate.matches(fluid, this, world)=true");
        System.out.println(recipe.toString());
      }
    }
//    CastingRecipe recipe = getWorld().getRecipeManager().getRecipe(TinkerSmeltery.castingRecipeType, this, this.world).orElse(null);
    return recipe;
  }

  public int initNewCasting(Fluid fluid, IFluidHandler.FluidAction action) {
    System.out.println(String.format("initNewCasting(%s, %s)", fluid.getRegistryName(), action.toString()));
    CastingRecipe recipe = findRecipe(fluid);
    if (recipe != null) {
      if (action == IFluidHandler.FluidAction.SIMULATE) {
        this.recipe = recipe;
        System.out.println(String.format("recipe=%s:%d::%s", recipe.getFluid().getRegistryName(), recipe.getFluidAmount(), recipe.getRecipeOutput().getItem().getRegistryName()));
      }
      return recipe.getFluidAmount();
    }
    return 0;
  }

  public void reset() {
    timer = 0;
    recipe = null;
    tank.setCapacity(0);
    tank.setFluid(FluidStack.EMPTY);
    tank.renderOffset = 0;
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
