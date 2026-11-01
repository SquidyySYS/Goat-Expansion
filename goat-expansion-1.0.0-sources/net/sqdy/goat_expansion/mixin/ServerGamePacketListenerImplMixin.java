package net.sqdy.goat_expansion.mixin;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"))
    private void onHandlePlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
            if (this.player.getVehicle() instanceof HasCustomInventoryScreen customScreenEntity) {
                customScreenEntity.openCustomInventoryScreen(this.player);
            }
        }
    }
}