package slimeknights.tconstruct.smeltery.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CastingRecipe implements IRecipe<IInventory> {
  protected final ResourceLocation id;
  protected final String group;
  private final FluidStack fluid;
  private final ItemStack result;
  protected final int coolingTime;


  public CastingRecipe(ResourceLocation idIn, String groupIn, @Nonnull FluidStack fluidIn, ItemStack result, int coolingTime) {
    this.id = idIn;
    this.group = groupIn;
    this.fluid = fluidIn;
    this.result = result;
    this.coolingTime = coolingTime;
  }

  // required
  @Override
  public boolean matches(IInventory inv, World worldIn) {
    return true;
  }

  public boolean matches(Fluid fluid, IInventory inv, World world) {
    return this.fluid.getFluid() == fluid;
  }
  @Override
  public ItemStack getCraftingResult(IInventory inv) {
    return result;
  }

  @Override
  public boolean canFit(int width, int height) {
    return true;
  }

  @Override
  public ItemStack getRecipeOutput() {
    return this.result;
  }

  @Override
  public ResourceLocation getId() {
    return this.id;
  }

  @Override
  public IRecipeSerializer<?> getSerializer() {
    return TinkerSmeltery.castingRecipeSerializer.get();
  }

  @Override
  public IRecipeType<?> getType() {
    return TinkerSmeltery.castingRecipeType;
  }

  public int getCoolingTime() {
    return this.coolingTime;
  }

  public int getFluidAmount() {
    return this.fluid.getAmount();
  }

  public Fluid getFluid() {
    return this.fluid.getFluid();
  }

  public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CastingRecipe> {
    private final Serializer.IFactory<CastingRecipe> factory;
    public Serializer(Serializer.IFactory<CastingRecipe> factoryIn) {
      this.factory = factoryIn;
    }

    @Override
    public CastingRecipe read(ResourceLocation recipeId, JsonObject json) {
      String s = JSONUtils.getString(json, "group", "");
      if (!json.has("fluid"))
        throw new JsonSyntaxException("Missing fluid input definition!");
      JsonObject jsonFluid = JSONUtils.getJsonObject(json, "fluid");
      FluidStack fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(JSONUtils.getString(jsonFluid, "name"))), JSONUtils.getInt(jsonFluid, "amount"));
      String resultString = JSONUtils.getString(json, "result");
      ResourceLocation res = new ResourceLocation(resultString);
      ItemStack item = new ItemStack(JSONUtils.getItem(json, "result"));
      int coolingtime = JSONUtils.getInt(json, "coolingtime", 200);
      return this.factory.create(recipeId, s, fluid, item, coolingtime);
    }

    @Nullable
    @Override
    public CastingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
      // TODO: Magic number go away
      String s = buffer.readString(32767);
      FluidStack fluidStack = FluidStack.readFromPacket(buffer);
      ItemStack output = buffer.readItemStack();
      int coolingtime = buffer.readInt();
      return this.factory.create(recipeId, s, fluidStack, output, coolingtime);
    }

    @Override
    public void write(PacketBuffer buffer, CastingRecipe recipe) {
      buffer.writeString(recipe.group);
      recipe.fluid.writeToPacket(buffer);
      buffer.writeItemStack(recipe.result);
      buffer.writeInt(recipe.coolingTime);
    }

    public interface IFactory<T> {
      T create(ResourceLocation idIn, String groupIn, @Nonnull FluidStack fluidIn, ItemStack result, int coolingTime);
    }
  }
}
