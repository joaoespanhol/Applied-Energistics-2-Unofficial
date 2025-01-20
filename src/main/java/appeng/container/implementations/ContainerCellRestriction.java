package appeng.container.implementations;

import appeng.helpers.ICellRestriction;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.parts.IPart;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;

public class ContainerCellRestriction extends AEBaseContainer {

    private final ICellRestriction Host;

    @SideOnly(Side.CLIENT)
    private MEGuiTextField typesField;

    @SideOnly(Side.CLIENT)
    private MEGuiTextField amountField;

    @SideOnly(Side.CLIENT)
    private Long totalBytes;

    @SideOnly(Side.CLIENT)
    private Integer totalTypes;

    @SideOnly(Side.CLIENT)
    private Integer perType;

    @GuiSync(69)
    String data;

    public ContainerCellRestriction(final InventoryPlayer ip, final ICellRestriction te) {
        super(ip, (TileEntity) (te instanceof TileEntity ? te : null), (IPart) (te instanceof IPart ? te : null));
        this.Host = te;
    }

    @SideOnly(Side.CLIENT)
    public void setAmountField(final MEGuiTextField f) {
        this.amountField = f;
    }

    @SideOnly(Side.CLIENT)
    public void setTypesField(final MEGuiTextField f) {
        this.typesField = f;
    }

    @SideOnly(Side.CLIENT)
    public void setCellData(Long totalBytes, Integer totalTypes, Integer perType) {
        this.totalBytes = totalBytes;
        this.totalTypes = totalTypes;
        this.perType = perType;
    }

    public void setCellRestriction(String data) {
        this.Host.setCellRestriction(data);
        this.data = data;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) this.data = this.Host.getCellData();
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(final String field, final Object oldValue, final Object newValue) {
        if (field.equals("CellWorkbench.cellRestriction") && (this.amountField != null && this.typesField != null)) {
            List<Object> newData = Arrays.asList(data.split(",", 4));
            this.totalBytes = (long) newData.get(0);
            this.totalTypes = (int) newData.get(1);
            this.perType = (int) newData.get(2);
            this.typesField.setText(newData.get(3).toString());
            this.amountField.setText(newData.get(4).toString());
        }
        super.onUpdate(field, oldValue, newValue);
    }
}