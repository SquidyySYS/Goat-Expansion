package net.sqdy.goat_expansion.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.sqdy.goat_expansion.GoatExpansion;
import net.sqdy.goat_expansion.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void onSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            @Nullable RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        ItemStack result = resultContainer.getItem(0);
        if (result.is(ModItems.IRON_PROSTHETIC_HORNS)) {
            String foundOwnerId = null;
            String foundGoatName = null;

            for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
                ItemStack ingredient = craftingContainer.getItem(i);
                if (ingredient.is(Items.GOAT_HORN)) {
                    CustomData customData = ingredient.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        String ownerId = tag.getString("GoatOwnerUUID").orElse("");
                        if (!ownerId.isEmpty()) {
                            foundOwnerId = ownerId;
                            foundGoatName = tag.getString("GoatOwnerName").orElse("Goat");
                            break;
                        }
                    }
                }
            }

            if (foundOwnerId != null) {
                final String finalId = foundOwnerId;
                final String finalName = foundGoatName;
                CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
                    tag.putString("GoatOwnerUUID", finalId);
                    tag.putString("GoatOwnerName", finalName);
                });
                result.set(DataComponents.CUSTOM_NAME, Component.literal("Протез рога (" + finalName + ")"));
                resultContainer.setItem(0, result);

                if (player instanceof ServerPlayer sp) {
                    AdvancementHolder adv = level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "craft_prosthetic"));
                    if (adv != null) {
                        var progress = sp.getAdvancements().getOrStartProgress(adv);
                        if (!progress.isDone()) {
                            for (String criterion : progress.getRemainingCriteria()) {
                                sp.getAdvancements().award(adv, criterion);
                            }
                        }
                    }
                }
            } else {
                resultContainer.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}