package net.sqdy.goat_expansion.item;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.sqdy.goat_expansion.screen.GoatPouchScreenHandler;

public class GoatPouchItem extends Item {

    public static final int POUCH_SIZE = 6;

    public GoatPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, playerInv, p) -> new GoatPouchScreenHandler(syncId, playerInv, stack),
                    stack.getHoverName() != null ? stack.getHoverName() : Component.literal("Козий мешочек")
            ));
        }
        return InteractionResult.SUCCESS;
    }

    public static NonNullList<ItemStack> getContents(ItemStack pouchStack) {
        NonNullList<ItemStack> list = NonNullList.withSize(POUCH_SIZE, ItemStack.EMPTY);
        if (pouchStack.isEmpty()) {
            return list;
        }
        ItemContainerContents contents = pouchStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        contents.copyInto(list);
        return list;
    }

    public static void setContents(ItemStack pouchStack, NonNullList<ItemStack> items) {
        if (!pouchStack.isEmpty()) {
            pouchStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }
}