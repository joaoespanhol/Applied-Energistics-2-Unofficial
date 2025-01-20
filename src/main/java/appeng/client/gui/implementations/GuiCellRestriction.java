package appeng.client.gui.implementations;

import java.io.IOException;

import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Keyboard;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCellRestriction;
import appeng.container.implementations.ContainerCellRestriction.cellData;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.ICellRestriction;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.misc.PartStorageBus;

public class GuiCellRestriction extends AEBaseGui {

    private MEGuiTextField amountField;
    private MEGuiTextField typesField;
    private cellData cellData;

    public GuiCellRestriction(InventoryPlayer ip, ICellRestriction obj) {
        super(new ContainerCellRestriction(ip, obj));
        this.xSize = 256;

        this.amountField = new MEGuiTextField(75, 12);
        this.typesField = new MEGuiTextField(40, 12);
        this.cellData = new cellData();

    }

    @Override
    public void initGui() {
        super.initGui();

        this.amountField.x = this.guiLeft + 64;
        this.amountField.y = this.guiTop + 32;

        this.typesField.x = this.guiLeft + 152;
        this.typesField.y = this.guiTop + 32;

        if (this.inventorySlots instanceof ContainerCellRestriction ccr) {
            ccr.setAmountField(this.amountField);
            ccr.setTypesField(this.typesField);
            ccr.setCellData(cellData);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    public String filterCellRestriction() {

        String types = this.typesField.getText();
        String amount = this.amountField.getText();

        int restrictionTypes = 0;
        long restrictionAmount = 0;

        try {
            restrictionTypes = Math.min(Integer.parseInt(types), cellData.getTotalTypes());
        } catch (Exception ignored) {
            //
        }
        try {
            restrictionAmount = Math.min(Long.parseLong(amount), cellData.getTotalBytes() * 8);
        } catch (Exception ignored) {
            //
        }
        this.typesField.setText(String.valueOf(restrictionTypes));
        this.amountField.setText(String.valueOf(restrictionAmount));
        return restrictionTypes + "," + restrictionAmount;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.CellRestriction.getLocal(), 58, 6, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 64, 23, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.Types.getLocal(), 152, 23, GuiColors.DefaultBlack.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/cellRestriction.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.amountField.drawTextBox();
        this.typesField.drawTextBox();
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        this.amountField.mouseClicked(xCoord, yCoord, btn);
        this.typesField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("cellRestriction", filterCellRestriction()));
            } catch (IOException e) {
                AELog.debug(e);
            }
            final Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
            GuiBridge OriginalGui = null;
            if (target instanceof PartStorageBus) OriginalGui = GuiBridge.GUI_STORAGEBUS;
            else if (target instanceof PartSharedItemBus) OriginalGui = GuiBridge.GUI_BUS;

            if (OriginalGui != null) NetworkHandler.instance.sendToServer(new PacketSwitchGuis(OriginalGui));
            else this.mc.thePlayer.closeScreen();

        } else if (!(this.amountField.textboxKeyTyped(character, key)
                || this.typesField.textboxKeyTyped(character, key))) {
                    super.keyTyped(character, key);
                    this.filterCellRestriction();
                }
    }
}
