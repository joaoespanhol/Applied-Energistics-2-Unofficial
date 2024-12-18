package appeng.parts.networking;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.client.texture.TextureUtils;
import appeng.parts.AEBasePart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartCreativeEnergy extends AEBasePart implements IAEPowerStorage {

    public PartCreativeEnergy(final ItemStack is) {
        super(is);
        this.getProxy().setIdlePowerUsage(0);
    }

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(6, 6, 10, 10, 10, 16);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(final IPartRenderHelper rh, final RenderBlocks renderer) {
        GL11.glTranslated(-0.2, -0.3, 0.0);

        rh.setTexture(this.getItemStack().getIconIndex());
        rh.setBounds(6.0f, 6.0f, 5.0f, 10.0f, 10.0f, 11.0f);
        rh.renderInventoryBox(renderer);
        rh.setTexture(null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(final int x, final int y, final int z, final IPartRenderHelper rh,
            final RenderBlocks renderer) {
        final IIcon myIcon = TextureUtils.checkTexture(this.getItemStack().getIconIndex());
        rh.setTexture(myIcon);
        rh.setBounds(6, 6, 10, 10, 10, 16);
        rh.renderBlock(x, y, z, renderer);
        rh.setTexture(null);
    }

    @Override
    public int cableConnectionRenderTo() {
        return 16;
    }

    @Override
    public double injectAEPower(final double amt, final Actionable mode) {
        return 0;
    }

    @Override
    public double getAEMaxPower() {
        return Long.MAX_VALUE / 10000;
    }

    @Override
    public double getAECurrentPower() {
        return Long.MAX_VALUE / 10000;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isInfinite() {
        return true;
    }

    @Override
    public double extractAEPower(final double amt, final Actionable mode, final PowerMultiplier pm) {
        return amt;
    }
}
