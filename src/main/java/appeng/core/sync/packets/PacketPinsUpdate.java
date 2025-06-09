package appeng.core.sync.packets;

import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import appeng.api.config.PinsState;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.IPinsHandler;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketPinsUpdate extends AppEngPacket {

    // input.
    @Nullable
    private final IAEItemStack[] list;

    private final PinsState state;

    public PacketPinsUpdate(final ByteBuf stream) throws IOException {
        IAEItemStack[] newList = new IAEItemStack[stream.readInt()];
        state = PinsState.values()[stream.readInt()];

        for (int i = 0; i < newList.length; i++) {
            if (stream.readBoolean()) {
                newList[i] = AEItemStack.loadItemStackFromPacket(stream);
            }
        }

        list = newList;
    }

    public PacketPinsUpdate(IAEItemStack[] arr, PinsState state) throws IOException {
        list = arr;
        this.state = state;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeInt(arr.length);
        data.writeInt(state.ordinal());

        for (IAEItemStack aeItemStack : arr) {
            if (aeItemStack != null) {
                data.writeBoolean(true);
                aeItemStack.writeToPacket(data);
            } else {
                data.writeBoolean(false);
            }
        }

        this.configureWrite(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

        if (gs instanceof IPinsHandler iph) {
            iph.setAEPins(list);
        }
    }

    @Override
    public void serverPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        final EntityPlayerMP sender = (EntityPlayerMP) player;
        if (sender.openContainer instanceof ContainerMEMonitorable container) {
            container.setPinsState(state);
        }
    }
}
