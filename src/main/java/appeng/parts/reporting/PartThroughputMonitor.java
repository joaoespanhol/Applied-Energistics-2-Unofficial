package appeng.parts.reporting;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.texture.CableBusTextures;
import appeng.helpers.Reflected;
import appeng.util.IWideReadableNumberConverter;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import io.netty.buffer.ByteBuf;

/**
 * @author MCTBL
 * @version rv3-beta-538-GTNH
 * @since rv3-beta-538-GTNH
 */
public class PartThroughputMonitor extends AbstractPartMonitor implements IGridTickable {

    private static final IWideReadableNumberConverter NUMBER_CONVERTER = ReadableNumberConverter.INSTANCE;

    private static final CableBusTextures FRONT_BRIGHT_ICON = CableBusTextures.PartThroughputMonitor_Bright;
    private static final CableBusTextures FRONT_DARK_ICON = CableBusTextures.PartThroughputMonitor_Dark;
    private static final CableBusTextures FRONT_COLORED_ICON = CableBusTextures.PartThroughputMonitor_Colored;
    private static final CableBusTextures FRONT_COLORED_ICON_LOCKED = CableBusTextures.PartThroughputMonitor_Dark_Locked;

    private static final String[] TIME_UNIT = { "/t", "/s", "/m", "/h" };
    private static final float[] NUMBER_MULTIPLIER = { 1, 20, 1_200, 72_000 };

    private double itemNumsChange;
    private int timeMode;
    private long lastStackSize;

    @Reflected
    public PartThroughputMonitor(final ItemStack is) {
        super(is);
        this.itemNumsChange = 0;
        this.timeMode = 0;
        this.lastStackSize = -1;
    }

    @Override
    public CableBusTextures getFrontBright() {
        return FRONT_BRIGHT_ICON;
    }

    @Override
    public CableBusTextures getFrontColored() {
        return this.isLocked() ? FRONT_COLORED_ICON_LOCKED : FRONT_COLORED_ICON;
    }

    @Override
    public CableBusTextures getFrontDark() {
        return FRONT_DARK_ICON;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.timeMode = data.getInteger("timeMode");
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("timeMode", this.timeMode);
    }

    @Override
    public void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeInt(this.timeMode);
        data.writeDouble(this.itemNumsChange);
    }

    @Override
    public boolean readFromStream(final ByteBuf data) throws IOException {
        boolean needRedraw = super.readFromStream(data);
        this.timeMode = data.readInt();
        this.itemNumsChange = data.readDouble();
        return needRedraw;
    }

    @Override
    public boolean onPartShiftActivate(final EntityPlayer player, final Vec3 pos) {
        if (Platform.isClient()) {
            return true;
        }

        if (!this.getProxy().isActive()) {
            return false;
        }

        if (!Platform.hasPermissions(this.getLocation(), player)) {
            return false;
        }

        final TileEntity te = this.getTile();
        final ItemStack eq = player.getCurrentEquippedItem();

        if (!Platform.isWrench(player, eq, te.xCoord, te.yCoord, te.zCoord) && this.isLocked()) {
            this.timeMode = (this.timeMode + TIME_UNIT.length - 1) % TIME_UNIT.length;
            return true;
        } else {
            return super.onPartShiftActivate(player, pos);
        }
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final Vec3 pos) {
        if (Platform.isClient()) {
            return true;
        }

        if (!this.getProxy().isActive()) {
            return false;
        }

        if (!Platform.hasPermissions(this.getLocation(), player)) {
            return false;
        }

        final TileEntity te = this.getTile();
        final ItemStack eq = player.getCurrentEquippedItem();

        if (!Platform.isWrench(player, eq, te.xCoord, te.yCoord, te.zCoord) && this.isLocked()) {
            this.timeMode = (this.timeMode + 1) % TIME_UNIT.length;
            return true;
        } else {
            return super.onPartActivate(player, pos);
        }
    }

    public void updateThroughput(int tick) {
        if (Platform.isClient()) {
            return;
        }

        if (this.getDisplayed() == null) {
            this.lastStackSize = -1;
            this.host.markForUpdate();
            return;
        } else {
            long nowStackSize = this.getDisplayed().getStackSize();
            if (this.lastStackSize != -1) {
                long changeStackSize = nowStackSize - this.lastStackSize;
                this.itemNumsChange = (changeStackSize * NUMBER_MULTIPLIER[this.timeMode]) / tick;
                this.host.markForUpdate();
            }
            this.lastStackSize = nowStackSize;
        }
    }

    @Override
    public void tesrRenderItemNumber(final IAEItemStack ais) {
        GL11.glTranslatef(0.0f, 0.14f, -0.24f);
        GL11.glScalef(1.0f / 120.0f, 1.0f / 120.0f, 1.0f / 120.0f);

        final long stackSize = ais.getStackSize();
        final String renderedStackSize = NUMBER_CONVERTER.toWideReadableForm(stackSize);

        final String renderedStackSizeChange = (this.itemNumsChange > 0 ? "+" : "")
                + (Platform.formatNumberLongRestrictedByWidth(this.itemNumsChange, 3))
                + (TIME_UNIT[this.timeMode]);

        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int width = fr.getStringWidth(renderedStackSize);
        GL11.glTranslatef(-0.5f * width, 0.0f, -1.0f);
        fr.drawString(renderedStackSize, 0, 0, 0);
        GL11.glTranslatef(+0.5f * width, fr.FONT_HEIGHT + 3, -1.0f);

        width = fr.getStringWidth(renderedStackSizeChange);
        GL11.glTranslatef(-0.5f * width, 0.0f, -1.0f);
        int color = 0;
        if (this.itemNumsChange < 0) {
            color = 0xFF0000;
        } else if (this.itemNumsChange > 0) {
            color = 0x17B66C;
        }
        fr.drawString(renderedStackSizeChange, 0, 0, color);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 100, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int TicksSinceLastCall) {
        this.updateThroughput(TicksSinceLastCall);
        return TickRateModulation.SAME;
    }

}
