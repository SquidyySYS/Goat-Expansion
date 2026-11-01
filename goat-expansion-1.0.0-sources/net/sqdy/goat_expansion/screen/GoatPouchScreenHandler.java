package net.sqdy.goat_expansion.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sqdy.goat_expansion.item.GoatPouchItem;

public class GoatPouchScreenHandler extends AbstractContainerMenu {

    private final ItemStack pouchStack;
    private final SimpleContainer pouchInventory;

    public GoatPouchScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ItemStack.EMPTY);
    }

    public GoatPouchScreenHandler(int syncId, Inventory playerInventory, ItemStack pouchStack) {
        super(ModScreenHandlers.GOAT_POUCH_SCREEN_HANDLER, syncId);
        this.pouchStack = pouchStack;

        // Автоматическое сохранение содержимого мешочка при любом изменении слотов
        this.pouchInventory = new SimpleContainer(GoatPouchItem.POUCH_SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                GoatPouchScreenHandler.this.saveToPouch();
            }
        };

        if (!pouchStack.isEmpty()) {
            NonNullList<ItemStack> items = GoatPouchItem.getContents(pouchStack);
            for (int i = 0; i < GoatPouchItem.POUCH_SIZE; i++) {
                this.pouchInventory.setItem(i, items.get(i));
            }
        }

        // 6 слотов мешочка (2 ряда по 3 слота по центру окна)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 3; ++col) {
                int index = col + row * 3;
                this.addSlot(new Slot(this.pouchInventory, index, 62 + col * 18, 26 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Нельзя класть мешочек внутрь другого мешочка
                        return !(stack.getItem() instanceof GoatPouchItem);
                    }
                });
            }
        }

        // Инвентарь игрока (3 ряда x 9 колонок)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Хотбар игрока (блокируем слот, в котором находится открытый мешочек)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    return this.getItem() != GoatPouchScreenHandler.this.pouchStack;
                }
            });
        }
    }

    private void saveToPouch() {
        if (!this.pouchStack.isEmpty()) {
            NonNullList<ItemStack> list = NonNullList.withSize(GoatPouchItem.POUCH_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < GoatPouchItem.POUCH_SIZE; i++) {
                list.set(i, this.pouchInventory.getItem(i));
            }
            GoatPouchItem.setContents(this.pouchStack, list);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            if (originalStack == this.pouchStack) {
                return ItemStack.EMPTY;
            }
            newStack = originalStack.copy();

            if (invSlot < 6) {
                if (!this.moveItemStackTo(originalStack, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GoatPouchItem) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(originalStack, 0, 6, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToPouch();
    }
}