package appeng.api.util;

import appeng.api.storage.data.IAEStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

public class ItemSearchDTO {

    public DimensionalCoord coord;
    public int cellSlot;
    public long itemCount;
    public ForgeDirection forward;
    public ForgeDirection up;
    public String name;

    public ItemSearchDTO(DimensionalCoord coord, IAEStack items, String name, int cellSlot, ForgeDirection forward, ForgeDirection up) {
        this.coord = coord;
        this.cellSlot = cellSlot;
        this.itemCount = items.getStackSize();
        this.name = name;
        this.forward = forward;
        this.up = up;
    }

    public ItemSearchDTO(DimensionalCoord coord, IAEStack items, String name) {
        this(coord, items, name,-1, ForgeDirection.UNKNOWN, ForgeDirection.UNKNOWN);
    }

    public ItemSearchDTO(final NBTTagCompound data) {
        this.readFromNBT(data);
    }

    public void writeToNBT(final NBTTagCompound data) {
        data.setInteger("dim", coord.dimId);
        data.setInteger("x", coord.x);
        data.setInteger("y", coord.y);
        data.setInteger("z", coord.z);
        data.setInteger("s", cellSlot);
        data.setLong("c", itemCount);
        data.setString("n", name);
        data.setString("f", this.forward.name());
        data.setString("u", this.up.name());
    }

    public static void writeListToNBT(final NBTTagCompound tag, List<ItemSearchDTO> list) {
        int i = 0;
        for (ItemSearchDTO d : list) {
            NBTTagCompound data = new NBTTagCompound();
            d.writeToNBT(data);
            tag.setTag("pos#" + i, data);
            i++;
        }
    }

    public static List<ItemSearchDTO> readAsListFromNBT(final NBTTagCompound tag) {
        List<ItemSearchDTO> list = new ArrayList<>();
        int i = 0;
        while (tag.hasKey("pos#" + i)) {
            NBTTagCompound data = tag.getCompoundTag("pos#" + i);
            list.add(new ItemSearchDTO(data));
            i++;
        }
        return list;
    }

    private void readFromNBT(final NBTTagCompound data) {
        int dim = data.getInteger("dim");
        int x = data.getInteger("x");
        int y = data.getInteger("y");
        int z = data.getInteger("z");
        this.name = data.getString("n");
        this.itemCount = data.getLong("c");
        this.cellSlot = data.getInteger("s");
        this.coord = new DimensionalCoord(x, y, z, dim);
        this.forward = ForgeDirection.valueOf(data.getString("f"));
        this.up = ForgeDirection.valueOf(data.getString("u"));
    }



}
