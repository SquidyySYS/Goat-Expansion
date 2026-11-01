package net.sqdy.goat_expansion.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.sqdy.goat_expansion.item.GoatPouchItem;
import net.sqdy.goat_expansion.item.ModItems;

import java.util.ArrayList;
import java.util.List;

public class GoatScreenHandler extends AbstractContainerMenu {

    private final Container goatInventory;
    private final SimpleContainer pouchStorage = new SimpleContainer(36);
    private final ContainerData syncedData;
    private final boolean isClientSide;
    private boolean isUpdatingPouches = false;

    public GoatScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(7), new SimpleContainerData(1));
    }

    public GoatScreenHandler(int syncId, Inventory playerInventory, Container goatInventory) {
        this(syncId, playerInventory, goatInventory, new SimpleContainerData(1));
    }

    public static boolean isGoatPouch(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.GOAT_POUCH) || stack.getItem() instanceof GoatPouchItem);
    }

    public List<ItemStack> getEquippedPouches() {
        List<ItemStack> pouches = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ItemStack stack = this.goatInventory.getItem(i);
            if (isGoatPouch(stack)) {
                pouches.add(stack);
            }
        }
        return pouches;
    }

    public int getEquippedPouchCount() {
        int localCount = getEquippedPouches().size();
        if (this.isClientSide) {
            return Math.max(localCount, this.syncedData.get(0));
        }
        return localCount;
    }

    public void refreshFromPouches() {
        if (this.isClientSide || this.isUpdatingPouches) return;
        this.isUpdatingPouches = true;
        List<ItemStack> pouches = getEquippedPouches();
        this.syncedData.set(0, pouches.size());

        for (int p = 0; p < 6; p++) {
            if (p < pouches.size()) {
                NonNullList<ItemStack> contents = GoatPouchItem.getContents(pouches.get(p));
                for (int s = 0; s < 6; s++) {
                    this.pouchStorage.setItem(p * 6 + s, contents.get(s));
                }
            } else {
                for (int s = 0; s < 6; s++) {
                    this.pouchStorage.setItem(p * 6 + s, ItemStack.EMPTY);
                }
            }
        }
        this.isUpdatingPouches = false;
    }

    public void saveToPouches() {
        if (this.isClientSide || this.isUpdatingPouches) return;
        this.isUpdatingPouches = true;
        List<ItemStack> pouches = getEquippedPouches();
        this.syncedData.set(0, pouches.size());

        for (int p = 0; p < pouches.size(); p++) {
            NonNullList<ItemStack> contents = NonNullList.withSize(6, ItemStack.EMPTY);
            for (int s = 0; s < 6; s++) {
                contents.set(s, this.pouchStorage.getItem(p * 6 + s));
            }
            GoatPouchItem.setContents(pouches.get(p), contents);
        }
        this.goatInventory.setChanged();
        this.isUpdatingPouches = false;
    }

    public GoatScreenHandler(int syncId, Inventory playerInventory, Container goatInventory, ContainerData data) {
        super(ModScreenHandlers.GOAT_SCREEN_HANDLER, syncId);
        this.goatInventory = goatInventory;
        this.syncedData = data;
        this.isClientSide = playerInventory.player.level().isClientSide();
        this.addDataSlots(this.syncedData);

        goatInventory.startOpen(playerInventory.player);
        refreshFromPouches();

        // 0: Слот седла слева
        this.addSlot(new Slot(goatInventory, 0, 8, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.GOAT_SADDLE);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // --- ВЕРХНИЙ РЯД (Слоты 1, 2, 3) ---
        this.addSlot(new Slot(goatInventory, 1, 95, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (goatInventory.getItem(2).getItem() instanceof ShieldItem) return false;
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        this.addSlot(new Slot(goatInventory, 2, 113, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.getItem() instanceof ShieldItem) {
                    return goatInventory.getItem(1).isEmpty() && goatInventory.getItem(3).isEmpty();
                }
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        this.addSlot(new Slot(goatInventory, 3, 131, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (goatInventory.getItem(2).getItem() instanceof ShieldItem) return false;
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        // --- НИЖНИЙ РЯД (Слоты 4, 5, 6) ---
        this.addSlot(new Slot(goatInventory, 4, 95, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (goatInventory.getItem(5).getItem() instanceof ShieldItem) return false;
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        this.addSlot(new Slot(goatInventory, 5, 113, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.getItem() instanceof ShieldItem) {
                    return goatInventory.getItem(4).isEmpty() && goatInventory.getItem(6).isEmpty();
                }
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        this.addSlot(new Slot(goatInventory, 6, 131, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (goatInventory.getItem(5).getItem() instanceof ShieldItem) return false;
                return isGoatPouch(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                refreshFromPouches();
            }
        });

        // Инвентарь игрока (слоты 7..33)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Хотбар игрока (слоты 34..42)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // --- ДИНАМИЧЕСКИЕ СЛОТЫ МЕШОЧКОВ СПРАВА (X = 177 + col*18, Y = 12 + row*18) ---
        for (int pouchIndex = 0; pouchIndex < 6; pouchIndex++) {
            for (int internalSlot = 0; internalSlot < 6; internalSlot++) {
                int row = pouchIndex * 2 + (internalSlot / 3);
                int col = internalSlot % 3;
                int slotX = 177 + col * 18;
                int slotY = 12 + row * 18;

                final int pIdx = pouchIndex;

                this.addSlot(new Slot(this.pouchStorage, pIdx * 6 + internalSlot, slotX, slotY) {
                    @Override
                    public boolean isActive() {
                        return pIdx < GoatScreenHandler.this.getEquippedPouchCount();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return isActive() && !isGoatPouch(stack);
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        GoatScreenHandler.this.saveToPouches();
                    }
                });
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.goatInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            int activePouchSlots = this.getEquippedPouchCount() * 6;

            if (invSlot < 7 || invSlot >= 43) {
                if (!this.moveItemStackTo(originalStack, 7, 43, true)) {
                    return ItemStack.EMPTY;
                }
                if (invSlot < 7) {
                    refreshFromPouches();
                } else {
                    saveToPouches();
                }
            } else {
                if (originalStack.is(ModItems.GOAT_SADDLE)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof ShieldItem) {
                    if (!this.moveItemStackTo(originalStack, 2, 3, false)
                            && !this.moveItemStackTo(originalStack, 5, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isGoatPouch(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 1, 7, false)) {
                        return ItemStack.EMPTY;
                    }
                    refreshFromPouches();
                } else if (activePouchSlots > 0) {
                    if (!this.moveItemStackTo(originalStack, 43, 43 + activePouchSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                    saveToPouches();
                } else {
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
        saveToPouches();
        this.goatInventory.stopOpen(player);
    }
}