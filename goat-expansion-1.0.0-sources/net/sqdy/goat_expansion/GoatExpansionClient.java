package net.sqdy.goat_expansion;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.sqdy.goat_expansion.client.model.GoatProstheticHornsModel;
import net.sqdy.goat_expansion.client.render.GoatIronHornLayer;
import net.sqdy.goat_expansion.client.render.GoatPouchLayer;
import net.sqdy.goat_expansion.client.render.GoatProstheticHornLayer;
import net.sqdy.goat_expansion.client.render.GoatShieldLayer;
import net.sqdy.goat_expansion.entity.GoatSaddleAccessor;
import net.sqdy.goat_expansion.screen.GoatPouchScreen;
import net.sqdy.goat_expansion.screen.GoatScreen;
import net.sqdy.goat_expansion.screen.ModScreenHandlers;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class GoatExpansionClient implements ClientModInitializer {
    public static final Identifier SADDLED_TEXTURE = Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "textures/entity/goat/goat_saddled.png");
    public static final Identifier POUCHED_TEXTURE = Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "textures/entity/goat/goat_pouched.png");

    public record ExtraGoatState(
            boolean saddled, boolean hasPouches,
            int capsDurability, int prosthL, int prosthR,
            int pouchMask, ItemStack leftShield, ItemStack rightShield
    ) {}

    private static final Map<GoatRenderState, ExtraGoatState> EXTRA_STATE_MAP = Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean isChargingStance = false;

    public static ItemStack getLeftShield(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null ? extra.leftShield() : ItemStack.EMPTY;
    }

    public static ItemStack getRightShield(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null ? extra.rightShield() : ItemStack.EMPTY;
    }

    public static int getPouchMask(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null ? extra.pouchMask() : 0;
    }

    public static int getCapsDurability(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null ? extra.capsDurability() : 0;
    }

    public static boolean hasIronCaps(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null && extra.capsDurability() > 0;
    }

    public static boolean hasProstheticHorns(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null && (extra.prosthL() > 0 || extra.prosthR() > 0);
    }

    public static boolean hasLeftProsthetic(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null && extra.prosthL() > 0;
    }

    public static boolean hasRightProsthetic(GoatRenderState state) {
        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
        return extra != null && extra.prosthR() > 0;
    }

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(
                GoatProstheticHornLayer.PROSTHETIC_HORN_LAYER,
                GoatProstheticHornsModel::createBodyLayer
        );

        MenuScreens.register(ModScreenHandlers.GOAT_SCREEN_HANDLER, GoatScreen::new);
        MenuScreens.register(ModScreenHandlers.GOAT_POUCH_SCREEN_HANDLER, GoatPouchScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof Goat) {
                if (mc.options.keySprint.isDown() && !mc.player.isSprinting()) {
                    mc.player.setSprinting(true);
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                }
                isChargingStance = mc.player.isSprinting()
                        && mc.screen == null
                        && InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_ALT);
            } else {
                isChargingStance = false;
            }
        });

        EntityRendererRegistry.register(EntityType.GOAT, (EntityRendererProvider.Context context) ->
                new GoatRenderer(context) {
                    {
                        this.addLayer(new GoatShieldLayer(this, context.getItemModelResolver()));
                        this.addLayer(new GoatPouchLayer(this, context.getItemModelResolver()));
                        this.addLayer(new GoatIronHornLayer(this));
                        this.addLayer(new GoatProstheticHornLayer(this, context.getModelSet()));
                    }

                    @Override
                    public void extractRenderState(Goat goat, GoatRenderState state, float partialTick) {
                        super.extractRenderState(goat, state, partialTick);
                        if (goat instanceof GoatSaddleAccessor accessor) {
                            EXTRA_STATE_MAP.put(state, new ExtraGoatState(
                                    accessor.goat_expansion$isSaddled(),
                                    accessor.goat_expansion$hasPouches(),
                                    accessor.goat_expansion$getCapsDurability(),
                                    accessor.goat_expansion$getProstheticLDurability(),
                                    accessor.goat_expansion$getProstheticRDurability(),
                                    accessor.goat_expansion$getPouchMask(),
                                    accessor.goat_expansion$getLeftShield(),
                                    accessor.goat_expansion$getRightShield()
                            ));
                        }
                    }

                    @Override
                    public Identifier getTextureLocation(GoatRenderState state) {
                        ExtraGoatState extra = EXTRA_STATE_MAP.get(state);
                        if (extra != null) {
                            if (extra.saddled()) return SADDLED_TEXTURE;
                            if (extra.hasPouches()) return POUCHED_TEXTURE;
                        }
                        return super.getTextureLocation(state);
                    }
                }
        );
    }
}