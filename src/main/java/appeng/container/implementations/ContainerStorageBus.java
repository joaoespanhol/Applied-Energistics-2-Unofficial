/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.container.implementations;

import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.ActionItems;
import appeng.api.config.FuzzyMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.OptionalSlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.parts.misc.PartStorageBus;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.IterationCounter;
import appeng.util.Platform;

public class ContainerStorageBus extends ContainerUpgradeable implements IConfigManagerHost {

    private final PartStorageBus storageBus;

    @GuiSync(3)
    public AccessRestriction rwMode = AccessRestriction.READ_WRITE;

    @GuiSync(4)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;

    @GuiSync(7)
    public YesNo stickyMode = YesNo.NO;

    public ContainerStorageBus(final InventoryPlayer ip, final PartStorageBus te) {
        super(ip, te);
        this.storageBus = te;
    }

    @Override
    protected int getHeight() {
        return 251;
    }

    @Override
    protected void setupConfig() {
        final int xo = 8;
        final int yo = 23 + 6;

        final IInventory config = this.getUpgradeable().getInventoryByName("config");
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(new OptionalSlotFakeTypeOnly(config, this, y * 9 + x, xo, yo, x, y, y));
            }
        }

        final IInventory upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        this.addSlotToContainer(
                (new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.UPGRADES,
                        upgrades,
                        0,
                        187,
                        8,
                        this.getInventoryPlayer())).setNotDraggable());
        this.addSlotToContainer(
                (new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.UPGRADES,
                        upgrades,
                        1,
                        187,
                        8 + 18,
                        this.getInventoryPlayer())).setNotDraggable());
        this.addSlotToContainer(
                (new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.UPGRADES,
                        upgrades,
                        2,
                        187,
                        8 + 18 * 2,
                        this.getInventoryPlayer())).setNotDraggable());
        this.addSlotToContainer(
                (new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.UPGRADES,
                        upgrades,
                        3,
                        187,
                        8 + 18 * 3,
                        this.getInventoryPlayer())).setNotDraggable());
        this.addSlotToContainer(
                (new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.UPGRADES,
                        upgrades,
                        4,
                        187,
                        8 + 18 * 4,
                        this.getInventoryPlayer())).setNotDraggable());
    }

    @Override
    protected boolean supportCapacity() {
        return true;
    }

    @Override
    public int availableUpgrades() {
        return 5;
    }

    private int iRow = 0;

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        if (Platform.isServer()) {
            this.setFuzzyMode((FuzzyMode) this.getUpgradeable().getConfigManager().getSetting(Settings.FUZZY_MODE));
            this.setReadWriteMode(
                    (AccessRestriction) this.getUpgradeable().getConfigManager().getSetting(Settings.ACCESS));
            this.setStorageFilter(
                    (StorageFilter) this.getUpgradeable().getConfigManager().getSetting(Settings.STORAGE_FILTER));
            this.setStickyMode((YesNo) this.getUpgradeable().getConfigManager().getSetting(Settings.STICKY_MODE));
        }
        final int upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY);

        if (upgrades > 0) { // sync filter slots
            boolean needSync = this.storageBus.needSyncGUI;
            this.storageBus.needSyncGUI = false;
            if (iRow >= upgrades) {
                iRow = 0;
            } else if (needSync) {
                iRow = upgrades; // to be sure that the last line is sent correctly (do during the next update)
            } else iRow = iRow + 1;

            IInventory inv = this.getUpgradeable().getInventoryByName("config");
            int from = needSync ? 18 : 9 + (9 * upgrades); // start at first filter slot or at specific row
            int to = needSync ? inv.getSizeInventory() : 18 + (9 * upgrades); // end at last filter slot or at end of
                                                                              // specific row
            for (; from < to; from++) {
                if (upgrades <= (from / 9 - 2)) {
                    break;
                }
                ItemStack stack = inv.getStackInSlot(from);
                if (stack == null) {
                    continue;
                }
                for (ICrafting crafter : this.crafters) {
                    boolean isChangingQuantityOnly = false;
                    if (crafter instanceof EntityPlayerMP playerMP && playerMP.isChangingQuantityOnly) {
                        isChangingQuantityOnly = true;
                        // necessary to ensure that the package is sent correctly
                        playerMP.isChangingQuantityOnly = false;
                    }
                    crafter.sendSlotContents(this, from, stack);
                    if (isChangingQuantityOnly && crafter instanceof EntityPlayerMP playerMP) {
                        playerMP.isChangingQuantityOnly = true;
                    }
                }
            }

        }
        this.standardDetectAndSendChanges();
    }

    @Override
    public boolean isSlotEnabled(final int idx) {
        if (this.getUpgradeable().getInstalledUpgrades(Upgrades.ORE_FILTER) > 0) return false;

        final int upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY);

        return upgrades > (idx - 2);
    }

    private static Iterator<IAEItemStack> PartitionIterator = null;
    private static final ConfigManager PartitionAction; // TODO: find a way to synchronize the client and server
    private  static boolean HasNext = false;
    static {
        PartitionAction = new ConfigManager((manager, settingName, newValue) -> {});
        PartitionAction.registerSetting(Settings.ACTIONS, ActionItems.WRENCH);
    }

    public void clear() {
        final IInventory inv = this.getUpgradeable().getInventoryByName("config");
        for (int x = 0; x < inv.getSizeInventory(); x++) {
            inv.setInventorySlotContents(x, null);
        }
        clearPartitionIterator();
        this.detectAndSendChanges();
    }

    private static void clearPartitionIterator() {
        if (PartitionIterator != null) {
            PartitionIterator = null;
            HasNext = false;
            PartitionAction.putSetting(Settings.ACTIONS, ActionItems.WRENCH);
        }
    }

    public void partition(boolean clearIterator) {
        if (clearIterator) {
            clearPartitionIterator();
            return;
        }
        final IInventory inv = this.getUpgradeable().getInventoryByName("config");

        final IMEInventory<IAEItemStack> cellInv = this.storageBus.getInternalHandler();

        if (cellInv == null) {
            clearPartitionIterator();
            PartitionAction.putSetting(Settings.ACTIONS, ActionItems.WRENCH);
            return;
        }

        if (PartitionIterator == null) {
            final IItemList<IAEItemStack> list = cellInv
                    .getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId());
            PartitionIterator = list.iterator();
            PartitionAction.putSetting(Settings.ACTIONS, ActionItems.CELL_RESTRICTION);
            HasNext = PartitionIterator.hasNext(); // cache HasNext, call next internally
        }

        for (int x = 0; x < inv.getSizeInventory(); x++) {
            if (PartitionIterator == null) {
                inv.setInventorySlotContents(x, null);
                continue;
            }
            // invPartitionIteratorHasnext = invPartitionIteratorHasnext || invPartitionIterator.hasNext();
            if (this.isSlotEnabled(x / 9)) {
                if (HasNext) {
                    final ItemStack is = PartitionIterator.next().getItemStack();
                    is.stackSize = 1;
                    HasNext = PartitionIterator.hasNext();
                    inv.setInventorySlotContents(x, is);
                } else {
                    clearPartitionIterator();
                    inv.setInventorySlotContents(x, null);
                }
            } else inv.setInventorySlotContents(x, null);

        }
        if (!HasNext)
            clearPartitionIterator();
        this.detectAndSendChanges();
    }

    public AccessRestriction getReadWriteMode() {
        return this.rwMode;
    }

    public StorageFilter getStorageFilter() {
        return this.storageFilter;
    }

    public ActionItems getPartitionMode() {
        return (ActionItems) PartitionAction.getSetting(Settings.ACTIONS);
    }

//    public void setPartitionMode(final ActionItems action) {
//        PartitionAction.putSetting(Settings.ACTIONS, action);
//    }

    private void setStorageFilter(final StorageFilter storageFilter) {
        this.storageFilter = storageFilter;
    }

    public YesNo getStickyMode() {
        return this.stickyMode;
    }

    private void setStickyMode(final YesNo stickyMode) {
        this.stickyMode = stickyMode;
    }

    private void setReadWriteMode(final AccessRestriction rwMode) {
        this.rwMode = rwMode;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {

    }
}
