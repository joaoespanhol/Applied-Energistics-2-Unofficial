package appeng.block.misc;

import appeng.block.AEBaseTileBlock;
import appeng.core.features.AEFeature;
import appeng.tile.misc.TilePatternOptimizationMatrix;
import net.minecraft.block.material.Material;

import java.util.EnumSet;

public class BlockPatternOptimizationMatrix extends AEBaseTileBlock {
    public BlockPatternOptimizationMatrix() {
        super(Material.iron);
        this.setTileEntity(TilePatternOptimizationMatrix.class);
        this.setFeature(EnumSet.of(AEFeature.PatternsOptimizer));
    }


}
