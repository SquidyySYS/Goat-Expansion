package net.sqdy.goat_expansion.entity;

import net.minecraft.world.item.ItemStack;

public interface GoatSaddleAccessor {
    boolean goat_expansion$isSaddled();
    default boolean goat_expansion$hasPouches() { return false; }
    default int goat_expansion$getPouchMask() { return 0; }
    
    // Булевы проверки для обратной совместимости слоев
    default boolean goat_expansion$hasIronCaps() { return false; }
    default boolean goat_expansion$hasProstheticHorns() { return false; }
    
    // Характеристики прочности
    default int goat_expansion$getCapsDurability() { return 0; }
    default int goat_expansion$getProstheticLDurability() { return 0; }
    default int goat_expansion$getProstheticRDurability() { return 0; }

    default void goat_expansion$setChargeTicks(int ticks) {}
    default int goat_expansion$getChargeTicks() { return 0; }
    default float goat_expansion$getEffectiveCharge() { return 0.0F; }
    default int goat_expansion$getStunTicks() { return 0; }
    default void goat_expansion$triggerWallStun() {}

    default ItemStack goat_expansion$getLeftShield() { return ItemStack.EMPTY; }
    default ItemStack goat_expansion$getRightShield() { return ItemStack.EMPTY; }
}