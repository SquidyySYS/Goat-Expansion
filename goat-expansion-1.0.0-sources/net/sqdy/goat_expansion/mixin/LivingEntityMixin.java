package net.sqdy.goat_expansion.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import net.sqdy.goat_expansion.GoatExpansion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(
        method = "hurtServer",
        at = @At("HEAD"),
        cancellable = true
    )
    private void goat_expansion$onHurtServer(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Goat goat && damageSource.getEntity() != null) {
            Vec3 attackerPos = damageSource.getEntity().position();
            Vec3 goatPos = goat.position();
            Vec3 relative = attackerPos.subtract(goatPos);

            Vec3 look = goat.getViewVector(1.0F);
            Vec3 left = new Vec3(-look.z, 0.0, look.x); // вектор влево от направления взгляда козы

            double dot = relative.dot(left);
            SimpleContainer inv = GoatExpansion.GOAT_INVENTORIES.get(goat.getUUID());

            if (inv != null) {
                boolean blocked = false;
                // Атака с левого фланга (dot > 0.1) и надет левый щит (слот 2)
                if (dot > 0.1 && inv.getItem(2).getItem() instanceof ShieldItem) {
                    blocked = true;
                }
                // Атака с правого фланга (dot < -0.1) и надет правый щит (слот 5)
                else if (dot < -0.1 && inv.getItem(5).getItem() instanceof ShieldItem) {
                    blocked = true;
                }

                if (blocked) {
                    goat.level().playSound(null, goat.getX(), goat.getY(), goat.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.CRIT, goat.getX(), goat.getY() + 0.5, goat.getZ(), 6, 0.2, 0.2, 0.2, 0.1);

                    // Если урон незначительный, отменяем получение урона полностью
                    if (amount <= 4.0F) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}