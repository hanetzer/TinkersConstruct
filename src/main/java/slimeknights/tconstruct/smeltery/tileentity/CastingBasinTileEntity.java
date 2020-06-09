package slimeknights.tconstruct.smeltery.tileentity;

import net.minecraft.tileentity.TileEntityType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public class CastingBasinTileEntity extends CastingTileEntity {

  public CastingBasinTileEntity() {
    this(TinkerSmeltery.basin.get());
  }
  public CastingBasinTileEntity(TileEntityType<?> tileEntityTypeIn) {
    super(tileEntityTypeIn);
  }
}
