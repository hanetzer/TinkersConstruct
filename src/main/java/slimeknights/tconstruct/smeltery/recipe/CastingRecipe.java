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
import net.minecraft.item.crafting.Ingredient;
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
  private final Ingredient cast;
  private final FluidStack fluid;
  private final ItemStack result;
  protected final int coolingTime;
  private final boolean consumed;


  public CastingRecipe(ResourceLocation idIn, String groupIn, @Nullable Ingredient ingredient, @Nonnull FluidStack fluidIn, ItemStack result, int coolingTime, boolean consumed) {
    this.id = idIn;
    this.group = groupIn;
    this.cast = ingredient;
    this.fluid = fluidIn;
    this.result = result;
    this.coolingTime = coolingTime;
    this.consumed = consumed;
  }

  // required
  @Override
  public boolean matches(IInventory inv, World worldIn) {
    return this.cast.test(inv.getStackInSlot(0));
  }

  public boolean matches(Fluid fluid, IInventory inv, World world) {
    return this.fluid.getFluid() == fluid && this.matches(inv, world);
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

  public boolean consumesCast() {
    return this.consumed;
  }
  public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CastingRecipe> {
    private final Serializer.IFactory<CastingRecipe> factory;
    public Serializer(Serializer.IFactory<CastingRecipe> factoryIn) {
      this.factory = factoryIn;
    }

    @Override
    public CastingRecipe read(ResourceLocation recipeId, JsonObject json) {
      Ingredient ingredient = null;
      String s = JSONUtils.getString(json, "group", "");
      boolean consumed = false;
      if (JSONUtils.getBoolean(json, "cast", false)) {
        JsonElement jsonelement = (JSONUtils.isJsonArray(json, "ingredient") ? JSONUtils.getJsonArray(json, "ingredient") : JSONUtils.getJsonObject(json, "ingredient"));
        ingredient = Ingredient.deserialize(jsonelement);
        consumed = JSONUtils.getBoolean(json, "consumed", false);
      }
      else {
        ingredient = Ingredient.EMPTY;
      }
      if (!json.has("fluid"))
        throw new JsonSyntaxException("Missing fluid input definition!");
      JsonObject jsonFluid = JSONUtils.getJsonObject(json, "fluid");
      FluidStack fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(JSONUtils.getString(jsonFluid, "name"))), JSONUtils.getInt(jsonFluid, "amount"));
      String resultString = JSONUtils.getString(json, "result");
      ResourceLocation res = new ResourceLocation(resultString);
      ItemStack item = new ItemStack(JSONUtils.getItem(json, "result"));
      int coolingtime = JSONUtils.getInt(json, "coolingtime", 200);
      return this.factory.create(recipeId, s, ingredient, fluid, item, coolingtime, consumed);
    }

    @Nullable
    @Override
    public CastingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
      // TODO: Magic number go away
      String s = buffer.readString(32767);
      Ingredient ingredient = Ingredient.read(buffer);
      FluidStack fluidStack = FluidStack.readFromPacket(buffer);
      boolean consumed = buffer.readBoolean();
      ItemStack output = buffer.readItemStack();
      int coolingtime = buffer.readInt();
      return this.factory.create(recipeId, s, ingredient, fluidStack, output, coolingtime, consumed);
    }

    @Override
    public void write(PacketBuffer buffer, CastingRecipe recipe) {
      buffer.writeString(recipe.group);
      recipe.cast.write(buffer);
      recipe.fluid.writeToPacket(buffer);
      buffer.writeItemStack(recipe.result);
      buffer.writeInt(recipe.coolingTime);
      buffer.writeBoolean(recipe.consumed);
    }

    public interface IFactory<T> {
      T create(ResourceLocation idIn, String groupIn, @Nullable Ingredient cast, @Nonnull FluidStack fluidIn, ItemStack result, int coolingTime, boolean consumed);
    }
  }
}
