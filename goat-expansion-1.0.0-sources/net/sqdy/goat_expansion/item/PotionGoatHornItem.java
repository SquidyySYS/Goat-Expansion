package net.sqdy.goat_expansion.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionGoatHornItem extends Item {
    private static final int DRINK_DURATION = 32;

    public PotionGoatHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player ? (Player) livingEntity : null;
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide()) {
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            contents.forEachEffect(effect -> {
                int duration = effect.getDuration() > 1 ? effect.getDuration() / 2 : effect.getDuration();
                int amplifier = Math.max(0, effect.getAmplifier());
                if (effect.getEffect().value().isInstantenous()) {
                    livingEntity.addEffect(new MobEffectInstance(effect.getEffect(), 1, amplifier / 2));
                } else {
                    livingEntity.addEffect(new MobEffectInstance(effect.getEffect(), duration, amplifier, effect.isAmbient(), effect.isVisible()));
                }
            }, 1.0f);
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        livingEntity.gameEvent(GameEvent.DRINK);
        return stack.isEmpty() ? new ItemStack(Items.GOAT_HORN) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
}