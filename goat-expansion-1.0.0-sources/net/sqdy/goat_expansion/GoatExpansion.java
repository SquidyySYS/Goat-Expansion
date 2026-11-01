package net.sqdy.goat_expansion;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sqdy.goat_expansion.entity.GoatSaddleAccessor;
import net.sqdy.goat_expansion.item.ModItems;
import net.sqdy.goat_expansion.screen.GoatScreenHandler;
import net.sqdy.goat_expansion.screen.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GoatExpansion implements ModInitializer {
    public static final String MOD_ID = "goat_expansion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<UUID, SimpleContainer> GOAT_INVENTORIES = new HashMap<>();
    public static final Set<UUID> TAMED_GOATS = new HashSet<>();
    private static final Map<UUID, Integer> FEED_COUNT = new HashMap<>();
    private static final Map<UUID, Integer> REQUIRED_CARROTS = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static final CustomPacketPayload.Type<ChargePayload> CHARGE_PAYLOAD_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "charge"));

    public record ChargePayload(int chargeTicks) implements CustomPacketPayload {
        public static final StreamCodec<ByteBuf, ChargePayload> CODEC =
                ByteBufCodecs.VAR_INT.map(ChargePayload::new, ChargePayload::chargeTicks);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return CHARGE_PAYLOAD_TYPE;
        }
    }

    public static boolean isSaddled(UUID goatId) {
        SimpleContainer inv = GOAT_INVENTORIES.get(goatId);
        return inv != null && !inv.getItem(0).isEmpty() && inv.getItem(0).is(ModItems.GOAT_SADDLE);
    }

    public static float getHealAmount(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 28.0F;
        if (stack.is(Items.GOLDEN_APPLE)) return 8.0F;
        if (stack.is(Items.HAY_BLOCK)) return 6.0F;
        if (stack.is(Items.GOLDEN_CARROT)) return 4.0F;
        if (stack.is(Items.APPLE) || stack.is(Items.CARROT)) return 2.0F;
        return 0.0F;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Goat Expansion: Initializing...");
        ModItems.registerModItems();
        ModScreenHandlers.register();

        PayloadTypeRegistry.serverboundPlay().register(CHARGE_PAYLOAD_TYPE, ChargePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CHARGE_PAYLOAD_TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().getVehicle() instanceof GoatSaddleAccessor accessor) {
                    if (payload.chargeTicks() == -1) {
                        accessor.goat_expansion$triggerWallStun();
                    } else {
                        accessor.goat_expansion$setChargeTicks(payload.chargeTicks());
                    }
                }
            });
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof Goat goat) || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            // Запрет приручения и надевания седла на козлят
            if (goat.isBaby()) {
                return InteractionResult.PASS;
            }

            ItemStack heldItem = player.getItemInHand(hand);
            UUID goatId = goat.getUUID();
            boolean isTamed = TAMED_GOATS.contains(goatId);
            SimpleContainer inv = GOAT_INVENTORIES.computeIfAbsent(goatId, id -> new SimpleContainer(7));

            if (heldItem.is(Items.GOLDEN_CARROT) && !isTamed) {
                if (!player.getAbilities().instabuild) heldItem.shrink(1);

                int req = REQUIRED_CARROTS.computeIfAbsent(goatId, k -> 3 + RANDOM.nextInt(8));
                int fed = FEED_COUNT.getOrDefault(goatId, 0) + 1;
                FEED_COUNT.put(goatId, fed);

                if (world instanceof ServerLevel serverLevel) {
                    world.playSound(null, goat.getX(), goat.getY(), goat.getZ(), SoundEvents.GOAT_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    if (fed >= req) {
                        TAMED_GOATS.add(goatId);
                        serverLevel.sendParticles(ParticleTypes.HEART, goat.getX(), goat.getY() + 0.6, goat.getZ(), 8, 0.3, 0.3, 0.3, 0.0);

                        var maxHealthAttr = goat.getAttribute(Attributes.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            maxHealthAttr.setBaseValue(18.0);
                            goat.setHealth(18.0f);
                        }
                    } else {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, goat.getX(), goat.getY() + 0.5, goat.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            float healAmount = getHealAmount(heldItem);
            if (healAmount > 0.0F && goat.getHealth() < goat.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                if (world instanceof ServerLevel serverLevel) {
                    goat.heal(healAmount);
                    world.playSound(null, goat.getX(), goat.getY(), goat.getZ(), SoundEvents.GOAT_EAT, SoundSource.NEUTRAL, 1.0f, 1.1f);
                    serverLevel.sendParticles(ParticleTypes.HEART, goat.getX(), goat.getY() + 0.7, goat.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
                }
                return InteractionResult.SUCCESS;
            }

            boolean isSneaking = player.isCrouching() || player.isShiftKeyDown();
            if (isTamed && (isSneaking || heldItem.is(Items.CHEST))) {
                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    openGoatMenu(serverPlayer, inv);
                }
                return InteractionResult.SUCCESS;
            }

            if (isTamed && heldItem.is(ModItems.GOAT_SADDLE) && inv.getItem(0).isEmpty()) {
                inv.setItem(0, heldItem.copyWithCount(1));
                var maxHealthAttr = goat.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(28.0);
                    goat.heal(10.0F);
                }
                if (!player.getAbilities().instabuild) heldItem.shrink(1);
                world.playSound(null, goat.getX(), goat.getY(), goat.getZ(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }

            boolean hasSaddle = isSaddled(goatId);
            if (isTamed && hasSaddle && heldItem.isEmpty() && !goat.isVehicle()) {
                if (!world.isClientSide()) {
                    player.startRiding(goat);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    public static void openGoatMenu(ServerPlayer serverPlayer, SimpleContainer inv) {
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInv, p) -> new GoatScreenHandler(syncId, playerInv, inv),
                Component.literal("Goat")
        ));
    }
}