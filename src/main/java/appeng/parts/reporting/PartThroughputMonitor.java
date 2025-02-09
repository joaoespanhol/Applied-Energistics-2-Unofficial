package appeng.parts.reporting;

import java.io.IOException;
import java.text.DecimalFormat;

import org.lwjgl.opengl.GL11;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.texture.CableBusTextures;
import appeng.helpers.Reflected;
import appeng.util.IWideReadableNumberConverter;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

/**
 * @author MCTBL
 * @version rv3-beta-538-GTNH
 * @since rv3-beta-538-GTNH
 */
public class PartThroughputMonitor extends AbstractPartMonitor {

    private static final IWideReadableNumberConverter NUMBER_CONVERTER = ReadableNumberConverter.INSTANCE;
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    private static final CableBusTextures FRONT_BRIGHT_ICON = CableBusTextures.PartThroughputMonitor_Bright;
    private static final CableBusTextures FRONT_DARK_ICON = CableBusTextures.PartThroughputMonitor_Dark;
    private static final CableBusTextures FRONT_COLORED_ICON = CableBusTextures.PartThroughputMonitor_Colored;
    private static final CableBusTextures FRONT_COLORED_ICON_LOCKED = CableBusTextures.PartThroughputMonitor_Dark_Locked;

    private long lastitemNums;
    private double itemNumsChange;
    private int timeMode;

    @Reflected
    public PartThroughputMonitor(final ItemStack is) {
        super(is);
        this.lastitemNums = 0;
        this.itemNumsChange = 0;
        this.timeMode = 0;
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
    }

    @Override
    public boolean readFromStream(final ByteBuf data) throws IOException {
        boolean needRedraw = super.readFromStream(data);
        this.timeMode = data.readInt();
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

        this.timeMode = this.timeMode == 0 ? 1 : 0;

        return true;
    }

    public void updateThroughput() {
        if (this.getDisplayed() != null) {
            long nowNums = ((IAEItemStack) this.getDisplayed()).getStackSize();
            this.itemNumsChange = (nowNums - lastitemNums);
            // If is tick mode
            if (this.timeMode == 1) {
                this.itemNumsChange *= 20;
            }
            this.lastitemNums = nowNums;
        } else {
            this.itemNumsChange = 0;
            this.lastitemNums = 0;
        }
        this.getHost().markForUpdate();
    }

    @Override
    public void tesrRenderItemNumber(final IAEItemStack ais) {
        GL11.glTranslatef(0.0f, 0.14f, -0.24f);
        GL11.glScalef(1.0f / 120.0f, 1.0f / 120.0f, 1.0f / 120.0f);

        final long stackSize = ais.getStackSize();
        final String renderedStackSize = NUMBER_CONVERTER.toWideReadableForm(stackSize);
        
        String renderedStackSizeChange = "";
        if(this.itemNumsChange >= 1000) {
        	renderedStackSizeChange = Platform.formatNumberLong((long)stackSize);
        }else {
        	renderedStackSizeChange = DF.format(this.itemNumsChange);
        }
        renderedStackSizeChange = (this.itemNumsChange > 0 ? "+" : "") + renderedStackSizeChange + (this.timeMode == 0 ? "/s" : "/t");

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

}
