/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.networking;

import java.util.EnumSet;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartRenderHelper;
import appeng.client.texture.TextureUtils;
import appeng.me.helpers.AENetworkProxy;
import appeng.parts.AEBasePart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartCreativeEnergy extends AEBasePart implements IAEPowerStorage {

    private final AENetworkProxy outerProxy = new AENetworkProxy(
            this,
            "outer",
            this.getProxy().getMachineRepresentation(),
            true);

    public PartCreativeEnergy(final ItemStack is) {
        super(is);
        this.getProxy().setIdlePowerUsage(0);
        this.getProxy().setFlags(GridFlags.CANNOT_CARRY);
        this.outerProxy.setIdlePowerUsage(0);
        this.outerProxy.setFlags(GridFlags.CANNOT_CARRY);
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
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.outerProxy.readFromNBT(extra);
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.outerProxy.writeToNBT(extra);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.outerProxy.invalidate();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.outerProxy.onReady();
    }

    @Override
    public void setPartHostInfo(final ForgeDirection side, final IPartHost host, final TileEntity tile) {
        super.setPartHostInfo(side, host, tile);
        this.outerProxy.setValidSides(EnumSet.of(side));
    }

    @Override
    public IGridNode getExternalFacingNode() {
        return this.outerProxy.getNode();
    }

    @Override
    public int cableConnectionRenderTo() {
        return 16;
    }

    @Override
    public void onPlacement(final EntityPlayer player, final ItemStack held, final ForgeDirection side) {
        super.onPlacement(player, held, side);
        this.outerProxy.setOwner(player);
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
