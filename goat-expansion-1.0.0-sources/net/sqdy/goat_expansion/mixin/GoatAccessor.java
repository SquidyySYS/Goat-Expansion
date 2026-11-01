package net.sqdy.goat_expansion.mixin;

import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Goat.class)
public interface GoatAccessor {
    @Invoker("setLeftHorn")
    void setLeftHorn(boolean hasHorn);

    @Invoker("setRightHorn")
    void setRightHorn(boolean hasHorn);
}