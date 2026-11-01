package net.sqdy.goat_expansion.mixin;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sqdy.goat_expansion.GoatExpansion;
import net.sqdy.goat_expansion.GoatExpansionClient;
import net.sqdy.goat_expansion.entity.GoatSaddleAccessor;
import net.sqdy.goat_expansion.item.ModItems;
import net.sqdy.goat_expansion.screen.GoatScreenHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(Goat.class)
public abstract class GoatEntityMixin extends Animal implements HasCustomInventoryScreen, PlayerRideableJumping, GoatSaddleAccessor {

    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_HAS_LEFT_HORN;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_HAS_RIGHT_HORN;

    @Unique private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> HAS_POUCHES = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Integer> IRON_CAPS_DURABILITY = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> PROSTHETIC_L_DURABILITY = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> PROSTHETIC_R_DURABILITY = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> POUCH_MASK = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> SYNCED_RAM_COOLDOWN = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> SYNCED_STUN_TICKS = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> STUN_COUNT = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<ItemStack> LEFT_SHIELD = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> RIGHT_SHIELD = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.ITEM_STACK);

    @Unique private static final Random RANDOM = new Random();
    @Unique private int carrotsFed = 0;
    @Unique private int carrotsNeeded = 0;
    @Unique private float playerJumpPendingScale = 0.0F;
    @Unique private int chargeTicks = 0;

    protected GoatEntityMixin(EntityType<? extends Animal> entityType, Level level) { super(entityType, level); }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    protected void onDefineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SADDLED, false);
        builder.define(HAS_POUCHES, false);
        builder.define(IRON_CAPS_DURABILITY, 0);
        builder.define(PROSTHETIC_L_DURABILITY, 0);
        builder.define(PROSTHETIC_R_DURABILITY, 0);
        builder.define(POUCH_MASK, 0);
        builder.define(SYNCED_RAM_COOLDOWN, 0);
        builder.define(SYNCED_STUN_TICKS, 0);
        builder.define(STUN_COUNT, 0);
        builder.define(LEFT_SHIELD, ItemStack.EMPTY);
        builder.define(RIGHT_SHIELD, ItemStack.EMPTY);
    }

    @Unique
    private void triggerAdvancement(ServerPlayer player, String id) {
        if (player.level() instanceof ServerLevel serverLevel) {
            AdvancementHolder adv = serverLevel.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("goat_expansion", id));
            if (adv == null) {
                adv = serverLevel.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("goat-expansion", id));
            }
            if (adv != null) {
                var progress = player.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    for (String criterion : progress.getRemainingCriteria()) {
                        player.getAdvancements().award(adv, criterion);
                    }
                }
            }
        }
    }

    @Unique
    private ItemStack createBoundGoatHorn() {
        ItemStack horn = new ItemStack(Items.GOAT_HORN);
        String uuidStr = this.getUUID().toString();
        String goatName = this.hasCustomName() && this.getCustomName() != null ? this.getCustomName().getString() : "Прирученная коза";
        CustomData.update(DataComponents.CUSTOM_DATA, horn, tag -> {
            tag.putString("GoatOwnerUUID", uuidStr);
            tag.putString("GoatOwnerName", goatName);
        });
        horn.set(DataComponents.CUSTOM_NAME, Component.literal("Рог козы (" + goatName + ")"));
        return horn;
    }

    @Unique
    private void checkHornStateAdvancements(@Nullable ServerPlayer rider) {
        if (rider == null) return;
        boolean hasNatL = this.entityData.get(DATA_HAS_LEFT_HORN);
        boolean hasNatR = this.entityData.get(DATA_HAS_RIGHT_HORN);
        int prosthL = this.entityData.get(PROSTHETIC_L_DURABILITY);
        int prosthR = this.entityData.get(PROSTHETIC_R_DURABILITY);

        int totalHorns = (hasNatL || prosthL > 0 ? 1 : 0) + (hasNatR || prosthR > 0 ? 1 : 0);

        if (totalHorns == 1) {
            triggerAdvancement(rider, "one_horn_goat");
        }

        if (totalHorns == 0 && this.hasCustomName() && this.getCustomName() != null) {
            String name = this.getCustomName().getString().trim().toLowerCase();
            if (name.equals("кузя") || name.equals("kuzya")) {
                triggerAdvancement(rider, "kuzya_lost_horns");
            }
        }
    }

    @Unique
    private void damageCaps(ServerLevel level, int amount, @Nullable ServerPlayer rider) {
        int current = this.entityData.get(IRON_CAPS_DURABILITY);
        if (current <= 0) return;

        if (RANDOM.nextFloat() < 0.18F) {
            this.entityData.set(IRON_CAPS_DURABILITY, 0);
            this.spawnAtLocation(level, ModItems.IRON_HORN_CAP);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.2F, 0.7F);
            if (rider != null) rider.sendSystemMessage(Component.literal("§eНасадка соскользнула с рога!"));
            return;
        }

        current -= amount;
        if (current <= 0) {
            this.entityData.set(IRON_CAPS_DURABILITY, 0);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.NEUTRAL, 1.2F, 0.8F);
        } else {
            this.entityData.set(IRON_CAPS_DURABILITY, current);
        }
    }

    @Unique
    private void damageProsthetic(ServerLevel level, boolean isLeft, int amount, @Nullable ServerPlayer rider) {
        EntityDataAccessor<Integer> accessor = isLeft ? PROSTHETIC_L_DURABILITY : PROSTHETIC_R_DURABILITY;
        int current = this.entityData.get(accessor);
        if (current <= 0) return;
        current -= amount;
        if (current <= 0) {
            this.entityData.set(accessor, 0);
            this.spawnAtLocation(level, createBoundGoatHorn());
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.NEUTRAL, 1.2F, 0.8F);
            checkHornStateAdvancements(rider);
        } else {
            this.entityData.set(accessor, current);
        }
    }

    @Unique
    private void removeOneNaturalHorn(ServerLevel level, @Nullable ServerPlayer rider) {
        if (this.entityData.get(DATA_HAS_LEFT_HORN)) {
            this.entityData.set(DATA_HAS_LEFT_HORN, false);
            this.spawnAtLocation(level, createBoundGoatHorn());
            checkHornStateAdvancements(rider);
        } else if (this.entityData.get(DATA_HAS_RIGHT_HORN)) {
            this.entityData.set(DATA_HAS_RIGHT_HORN, false);
            this.spawnAtLocation(level, createBoundGoatHorn());
            checkHornStateAdvancements(rider);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        SimpleContainer inv = GoatExpansion.GOAT_INVENTORIES.remove(this.getUUID());
        if (inv != null) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.getItem(i).isEmpty()) this.spawnAtLocation(level, inv.getItem(i));
            }
        }
        if (this.entityData.get(IRON_CAPS_DURABILITY) > 0) this.spawnAtLocation(level, ModItems.IRON_HORN_CAP);
        if (this.entityData.get(PROSTHETIC_L_DURABILITY) > 0) this.spawnAtLocation(level, createBoundGoatHorn());
        if (this.entityData.get(PROSTHETIC_R_DURABILITY) > 0) this.spawnAtLocation(level, createBoundGoatHorn());
        GoatExpansion.TAMED_GOATS.remove(this.getUUID());
    }

    @Override public boolean goat_expansion$isSaddled() { return this.entityData.get(SADDLED); }
    public void setModSaddled(boolean saddled) { this.entityData.set(SADDLED, saddled); }
    @Override public boolean goat_expansion$hasPouches() { return this.entityData.get(HAS_POUCHES); }
    @Override public int goat_expansion$getPouchMask() { return this.entityData.get(POUCH_MASK); }
    @Override public ItemStack goat_expansion$getLeftShield() { return this.entityData.get(LEFT_SHIELD); }
    @Override public ItemStack goat_expansion$getRightShield() { return this.entityData.get(RIGHT_SHIELD); }

    @Override public boolean goat_expansion$hasIronCaps() { return this.entityData.get(IRON_CAPS_DURABILITY) > 0; }
    @Override public boolean goat_expansion$hasProstheticHorns() { return this.entityData.get(PROSTHETIC_L_DURABILITY) > 0 || this.entityData.get(PROSTHETIC_R_DURABILITY) > 0; }

    @Override public int goat_expansion$getCapsDurability() { return this.entityData.get(IRON_CAPS_DURABILITY); }
    @Override public int goat_expansion$getProstheticLDurability() { return this.entityData.get(PROSTHETIC_L_DURABILITY); }
    @Override public int goat_expansion$getProstheticRDurability() { return this.entityData.get(PROSTHETIC_R_DURABILITY); }

    @Override
    public void goat_expansion$setChargeTicks(int ticks) {
        this.chargeTicks = (this.entityData.get(SYNCED_RAM_COOLDOWN) <= 0 && this.entityData.get(SYNCED_STUN_TICKS) <= 0) ? ticks : 0;
    }

    @Override public int goat_expansion$getChargeTicks() { return this.chargeTicks; }
    @Override public int goat_expansion$getStunTicks() { return this.entityData.get(SYNCED_STUN_TICKS); }

    @Override
    public float goat_expansion$getEffectiveCharge() {
        if (this.chargeTicks <= 0) return 0.0F;
        if (this.chargeTicks <= 32) return (this.chargeTicks / 32.0F) * 0.80F;
        int cycle = (this.chargeTicks - 32) % 26;
        if (cycle <= 12) return 0.80F + (cycle / 12.0F) * 0.20F;
        else if (cycle <= 15) return 1.00F;
        else if (cycle <= 22) return 1.00F - ((cycle - 15) / 7.0F) * 0.20F;
        else return 0.80F;
    }

    @Override
    public void goat_expansion$triggerWallStun() {
        boolean hasNatL = this.entityData.get(DATA_HAS_LEFT_HORN);
        boolean hasNatR = this.entityData.get(DATA_HAS_RIGHT_HORN);
        int prosthL = this.entityData.get(PROSTHETIC_L_DURABILITY);
        int prosthR = this.entityData.get(PROSTHETIC_R_DURABILITY);
        int prosthCount = (prosthL > 0 ? 1 : 0) + (prosthR > 0 ? 1 : 0);
        boolean noHornsAtAll = !hasNatL && !hasNatR && prosthCount == 0;
        boolean isMaxCharge = this.goat_expansion$getEffectiveCharge() >= 0.90F;

        int stunTicks = 30;
        if (prosthCount > 0) stunTicks = 40;
        if (noHornsAtAll) stunTicks = 45;

        this.entityData.set(SYNCED_STUN_TICKS, stunTicks);
        this.chargeTicks = 0;
        this.setDeltaMovement(0.0, 0.1, 0.0);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GOAT_RAM_IMPACT, SoundSource.NEUTRAL, 2.0F, 0.6F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GOAT_SCREAMING_HURT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        if (this.level() instanceof ServerLevel serverLevel) {
            ServerPlayer rider = (this.getControllingPassenger() instanceof ServerPlayer sp) ? sp : null;

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.6, this.getZ(), 10, 0.3, 0.2, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY() + 0.3, this.getZ(), 8, 0.3, 0.2, 0.3, 0.1);

            float selfDamage = 0.0F;
            if (isMaxCharge) {
                if (noHornsAtAll) selfDamage = 2.0F;
                else if (prosthCount > 0) {
                    selfDamage = 3.0F;
                    if (rider != null) triggerAdvancement(rider, "wall_crash_prosthetic");
                }
            }
            if (selfDamage > 0.0F) this.hurtServer(serverLevel, this.damageSources().flyIntoWall(), selfDamage);

            if (this.entityData.get(IRON_CAPS_DURABILITY) > 0) damageCaps(serverLevel, 2, rider);
            if (prosthL > 0) damageProsthetic(serverLevel, true, 2, rider);
            if (prosthR > 0) damageProsthetic(serverLevel, false, 2, rider);

            int stuns = this.entityData.get(STUN_COUNT) + 1;
            if (stuns >= 3) {
                this.entityData.set(STUN_COUNT, 0);
                if (hasNatL || hasNatR) removeOneNaturalHorn(serverLevel, rider);
            } else {
                this.entityData.set(STUN_COUNT, stuns);
            }
        }
    }

    @Unique public SimpleContainer getGoatInventory() { return GoatExpansion.GOAT_INVENTORIES.computeIfAbsent(this.getUUID(), id -> new SimpleContainer(7)); }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isInvulnerableTo(level, source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount > 0.0F) {
            if (source.getEntity() != null && source.getEntity() != this) {
                SimpleContainer inv = this.getGoatInventory();
                ItemStack leftShield = inv.getItem(2);
                ItemStack rightShield = inv.getItem(5);
                boolean hasLeftShield = leftShield.getItem() instanceof ShieldItem;
                boolean hasRightShield = rightShield.getItem() instanceof ShieldItem;

                Vec3 sourcePos = source.getSourcePosition() == null ? source.getEntity().position() : source.getSourcePosition();
                if (sourcePos != null && (hasLeftShield || hasRightShield)) {
                    Vec3 toAttacker = sourcePos.subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
                    float bodyYawRad = this.yBodyRot * ((float) Math.PI / 180.0F);
                    Vec3 forward = new Vec3(-Mth.sin(bodyYawRad), 0.0, Mth.cos(bodyYawRad));
                    double sideCross = forward.x * toAttacker.z - forward.z * toAttacker.x;

                    int hitShieldSlot = -1;
                    ItemStack activeShield = ItemStack.EMPTY;
                    if (sideCross > 0.35D && hasLeftShield) { hitShieldSlot = 2; activeShield = leftShield; }
                    else if (sideCross < -0.35D && hasRightShield) { hitShieldSlot = 5; activeShield = rightShield; }

                    if (hitShieldSlot != -1 && !activeShield.isEmpty()) {
                        boolean isAxeAttack = source.getDirectEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().is(ItemTags.AXES);
                        int durabilityLoss = 1 + Mth.floor(amount) + (isAxeAttack ? 3 : 0);
                        ServerPlayer rider = (this.getControllingPassenger() instanceof ServerPlayer sp) ? sp : null;
                        activeShield.hurtAndBreak(durabilityLoss, level, rider, item -> level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BREAK.value(), SoundSource.NEUTRAL, 1.0F, 0.9F));
                        if (activeShield.isEmpty()) inv.setItem(hitShieldSlot, ItemStack.EMPTY);
                        amount *= isAxeAttack ? 0.85F : 0.65F;
                        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.NEUTRAL, 1.0F, 0.9F);
                    }
                }
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSaveData(ValueOutput output, CallbackInfo ci) {
        output.store("GoatTamed", Codec.BOOL, GoatExpansion.TAMED_GOATS.contains(this.getUUID()));
        output.store("GoatSaddled", Codec.BOOL, this.goat_expansion$isSaddled());
        output.store("GoatCapsDurability", Codec.INT, this.entityData.get(IRON_CAPS_DURABILITY));
        output.store("GoatProsthLDurability", Codec.INT, this.entityData.get(PROSTHETIC_L_DURABILITY));
        output.store("GoatProsthRDurability", Codec.INT, this.entityData.get(PROSTHETIC_R_DURABILITY));
        output.store("GoatStuns", Codec.INT, this.entityData.get(STUN_COUNT));

        SimpleContainer inv = this.getGoatInventory();
        CompoundTag invContainerTag = new CompoundTag();
        ListTag listTag = new ListTag();
        var ops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                final int slot = i;
                ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().ifPresent(tag -> {
                    if (tag instanceof CompoundTag itemCompound) {
                        itemCompound.putByte("Slot", (byte) slot);
                        listTag.add(itemCompound);
                    }
                });
            }
        }
        invContainerTag.put("Items", listTag);
        output.store("GoatInventory", CompoundTag.CODEC, invContainerTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoadData(ValueInput input, CallbackInfo ci) {
        if (input.read("GoatTamed", Codec.BOOL).orElse(false)) GoatExpansion.TAMED_GOATS.add(this.getUUID());
        this.setModSaddled(input.read("GoatSaddled", Codec.BOOL).orElse(false));
        this.entityData.set(IRON_CAPS_DURABILITY, input.read("GoatCapsDurability", Codec.INT).orElse(0));
        this.entityData.set(PROSTHETIC_L_DURABILITY, input.read("GoatProsthLDurability", Codec.INT).orElse(0));
        this.entityData.set(PROSTHETIC_R_DURABILITY, input.read("GoatProsthRDurability", Codec.INT).orElse(0));
        this.entityData.set(STUN_COUNT, input.read("GoatStuns", Codec.INT).orElse(0));

        var ops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        input.read("GoatInventory", CompoundTag.CODEC).ifPresent(tag -> tag.getList("Items").ifPresent(listTag -> {
            SimpleContainer inv = this.getGoatInventory();
            for (int i = 0; i < listTag.size(); ++i) {
                listTag.getCompound(i).ifPresent(itemTag -> {
                    int slot = itemTag.getByte("Slot").orElse((byte) 0) & 255;
                    if (slot < inv.getContainerSize()) ItemStack.OPTIONAL_CODEC.parse(ops, itemTag).result().ifPresent(stack -> inv.setItem(slot, stack));
                });
            }
        }));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (this.getTicksFrozen() > 0) this.setTicksFrozen(0);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            SimpleContainer inv = this.getGoatInventory();
            if (this.isBaby() && !inv.getItem(0).isEmpty()) this.spawnAtLocation(serverLevel, inv.removeItemNoUpdate(0));

            boolean hasSaddleItem = !this.isBaby() && !inv.getItem(0).isEmpty();
            if (this.goat_expansion$isSaddled() != hasSaddleItem) this.setModSaddled(hasSaddleItem);

            int mask = 0;
            for (int i = 1; i <= 6; i++) {
                if (GoatScreenHandler.isGoatPouch(inv.getItem(i))) mask |= (1 << (i - 1));
            }
            if (this.entityData.get(HAS_POUCHES) != (mask != 0)) this.entityData.set(HAS_POUCHES, mask != 0);
            if (this.entityData.get(POUCH_MASK) != mask) this.entityData.set(POUCH_MASK, mask);

            if (GoatExpansion.TAMED_GOATS.contains(this.getUUID())) {
                var maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    double targetMaxHealth = hasSaddleItem ? 28.0 : 18.0;
                    if (maxHealthAttr.getBaseValue() != targetMaxHealth) {
                        double oldMax = maxHealthAttr.getBaseValue();
                        maxHealthAttr.setBaseValue(targetMaxHealth);
                        if (targetMaxHealth > oldMax) this.heal((float) (targetMaxHealth - oldMax));
                        else if (this.getHealth() > (float) targetMaxHealth) this.setHealth((float) targetMaxHealth);
                    }
                }
            }

            ItemStack sLeft = inv.getItem(2).getItem() instanceof ShieldItem ? inv.getItem(2) : ItemStack.EMPTY;
            ItemStack sRight = inv.getItem(5).getItem() instanceof ShieldItem ? inv.getItem(5) : ItemStack.EMPTY;
            if (!ItemStack.matches(this.entityData.get(LEFT_SHIELD), sLeft)) this.entityData.set(LEFT_SHIELD, sLeft.copy());
            if (!ItemStack.matches(this.entityData.get(RIGHT_SHIELD), sRight)) this.entityData.set(RIGHT_SHIELD, sRight.copy());

            int cooldown = this.entityData.get(SYNCED_RAM_COOLDOWN);
            if (cooldown > 0) this.entityData.set(SYNCED_RAM_COOLDOWN, cooldown - 1);
            int stun = this.entityData.get(SYNCED_STUN_TICKS);
            if (stun > 0) {
                this.entityData.set(SYNCED_STUN_TICKS, stun - 1);
                if (stun % 5 == 0) serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY() + 0.25, this.getZ(), 4, 0.25, 0.1, 0.25, 0.0);
            }

            if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
                Goat selfGoat = (Goat) (Object) this;
                boolean isScreaming = selfGoat.isScreamingGoat();
                boolean hasNatL = this.entityData.get(DATA_HAS_LEFT_HORN);
                boolean hasNatR = this.entityData.get(DATA_HAS_RIGHT_HORN);
                int prosthL = this.entityData.get(PROSTHETIC_L_DURABILITY);
                int prosthR = this.entityData.get(PROSTHETIC_R_DURABILITY);
                int prosthCount = (prosthL > 0 ? 1 : 0) + (prosthR > 0 ? 1 : 0);
                boolean hasCaps = this.entityData.get(IRON_CAPS_DURABILITY) > 0;
                boolean noHornsAtAll = !hasNatL && !hasNatR && prosthCount == 0;

                if (this.chargeTicks >= 32 && stun <= 0) {
                    Vec3 headLook = player.getLookAngle();
                    if (this.chargeTicks == 32 || (this.chargeTicks - 32) % 26 == 0) serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), isScreaming ? SoundEvents.GOAT_SCREAMING_PREPARE_RAM : SoundEvents.GOAT_PREPARE_RAM, SoundSource.NEUTRAL, 0.9F, 1.1F);
                    if (this.chargeTicks % 4 == 0) serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX() + headLook.x * 0.7D, this.getEyeY() - 0.1D, this.getZ() + headLook.z * 0.7D, 2, 0.1D, 0.05D, 0.1D, 0.01D);
                }

                if (this.chargeTicks >= 3 && cooldown <= 0 && stun <= 0) {
                    float effectiveCharge = this.goat_expansion$getEffectiveCharge();
                    Vec3 look = player.getLookAngle();
                    AABB hitBox = this.getBoundingBox().inflate(0.75D, 0.35D, 0.75D).move(look.x * 0.65D, 0.0D, look.z * 0.65D);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e != player && e.isAlive());

                    if (!targets.isEmpty()) {
                        float damage;
                        if (noHornsAtAll) {
                            damage = Math.min(7.0F, 1.0F + effectiveCharge * 6.0F);
                        } else {
                            float baseDmg = Math.max(1.0F, effectiveCharge * 10.0F);
                            float hornBonus = prosthCount > 0 ? (prosthCount * 2.25F) : (hasCaps ? 4.0F : 0.0F);
                            damage = baseDmg + hornBonus + (isScreaming ? 1.5F : 0.0F);
                        }

                        float knockbackBoost = (isScreaming ? 0.35F : 0.0F) + (prosthCount > 0 ? 0.25F : 0.0F);
                        ServerPlayer rider = (player instanceof ServerPlayer sp) ? sp : null;

                        for (LivingEntity target : targets) {
                            if (target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
                                float shieldDamage = prosthCount > 0 ? 10.0F : (hasCaps ? 8.0F : (!noHornsAtAll ? 3.0F : 0.5F));
                                targetPlayer.hurtServer(serverLevel, this.damageSources().generic(), shieldDamage);
                                targetPlayer.knockback(0.45F + knockbackBoost * 0.5F, -look.x, -look.z);
                                targetPlayer.hurtMarked = true;
                                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
                            } else {
                                target.hurtServer(serverLevel, this.damageSources().mobAttack(this), damage);
                                target.knockback(0.55F + effectiveCharge * 0.90F + knockbackBoost, -look.x, -look.z);
                                target.hurtMarked = true;
                                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), isScreaming ? SoundEvents.GOAT_SCREAMING_RAM_IMPACT : SoundEvents.GOAT_RAM_IMPACT, SoundSource.NEUTRAL, 1.5F, 1.0F);

                                if (rider != null && prosthCount == 2 && effectiveCharge >= 0.90F) {
                                    if (!target.isAlive() || target.getHealth() <= 0.0F) {
                                        triggerAdvancement(rider, "shock_therapy");
                                    }
                                }
                            }
                        }

                        if (hasCaps) damageCaps(serverLevel, 1, rider);
                        if (prosthL > 0) damageProsthetic(serverLevel, true, 1, rider);
                        if (prosthR > 0) damageProsthetic(serverLevel, false, 1, rider);

                        serverLevel.sendParticles(ParticleTypes.CRIT, this.getX() + look.x * 0.8, this.getY() + 0.6, this.getZ() + look.z * 0.8, 12, 0.25, 0.25, 0.25, 0.1);
                        this.chargeTicks = 0;
                        this.entityData.set(SYNCED_RAM_COOLDOWN, 18);
                    }
                }
            } else {
                this.chargeTicks = 0;
            }
        }
    }

    @Override public void onPlayerJump(int jumpPower) { this.playerJumpPendingScale = jumpPower >= 90 ? 1.0F : 0.4F + 0.6F * (float) Math.max(0, jumpPower) / 90.0F; }
    @Override public boolean canJump() { return this.goat_expansion$isSaddled() && this.goat_expansion$getStunTicks() <= 0; }
    @Override public void handleStartJump(int jumpPower) {}
    @Override public void handleStopJump() {}

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (this.isBaby()) return;
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((syncId, playerInv, p) -> new GoatScreenHandler(syncId, playerInv, this.getGoatInventory()), Component.literal("Goat")));
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player && this.goat_expansion$isSaddled() && !this.isBaby()) return player;
        return null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (this.goat_expansion$getStunTicks() > 0) return Vec3.ZERO;
        return new Vec3(player.xxa * 0.5F, 0.0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        if (this.goat_expansion$getStunTicks() > 0) return 0.0F;
        float baseSpeed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        int prosthCount = (this.entityData.get(PROSTHETIC_L_DURABILITY) > 0 ? 1 : 0) + (this.entityData.get(PROSTHETIC_R_DURABILITY) > 0 ? 1 : 0);
        float weightMultiplier = prosthCount == 2 ? 0.85F : (prosthCount == 1 ? 0.92F : 1.0F);

        if (player.isSprinting() && this.chargeTicks > 0) return baseSpeed * (1.15F + this.goat_expansion$getEffectiveCharge() * 0.35F) * weightMultiplier;
        if (player.isSprinting()) return baseSpeed * 1.15F * weightMultiplier;
        return baseSpeed * 0.75F * weightMultiplier;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (this.goat_expansion$getStunTicks() > 0) {
                this.chargeTicks = 0;
                super.travel(Vec3.ZERO);
                return;
            }

            this.setYRot(player.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(player.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;

            float strafe = player.xxa * 0.5F;
            float forward = player.zza;
            if (forward <= 0.0F) forward *= 0.25F;

            if (this.playerJumpPendingScale > 0.0F && this.onGround()) {
                double jumpStrength = 0.76D * (double) this.playerJumpPendingScale;
                this.setDeltaMovement(this.getDeltaMovement().x, jumpStrength, this.getDeltaMovement().z);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GOAT_LONG_JUMP, SoundSource.NEUTRAL, 1.0F, 1.0F);
                this.playerJumpPendingScale = 0.0F;
            }

            boolean isClientCharging = false;
            if (this.level().isClientSide()) {
                int cooldown = this.entityData.get(SYNCED_RAM_COOLDOWN);
                isClientCharging = player.isSprinting() && GoatExpansionClient.isChargingStance && forward > 0.1F && cooldown <= 0;
                this.chargeTicks = isClientCharging ? this.chargeTicks + 1 : 0;
                ClientPlayNetworking.send(new GoatExpansion.ChargePayload(this.chargeTicks));
            }

            this.setSpeed(this.getRiddenSpeed(player));
            super.travel(new Vec3(strafe, travelVector.y, forward));

            if (this.level().isClientSide() && isClientCharging && this.horizontalCollision) {
                Vec3 look = player.getLookAngle();
                BlockPos frontPos = BlockPos.containing(this.getX() + look.x * 0.85D, this.getY() + 0.5D, this.getZ() + look.z * 0.85D);
                BlockPos headPos = BlockPos.containing(this.getX() + look.x * 0.85D, this.getY() + 1.1D, this.getZ() + look.z * 0.85D);
                BlockState hitState = this.level().getBlockState(frontPos);
                BlockState headState = this.level().getBlockState(headPos);

                if (this.chargeTicks >= 20 && (isHardBlock(hitState) || isHardBlock(headState))) {
                    this.chargeTicks = 0;
                    this.setDeltaMovement(0.0, 0.1, 0.0);
                    ClientPlayNetworking.send(new GoatExpansion.ChargePayload(-1));
                } else {
                    this.chargeTicks = 0;
                    ClientPlayNetworking.send(new GoatExpansion.ChargePayload(0));
                }
            }
            return;
        }
        super.travel(travelVector);
    }

    @Unique
    private boolean isHardBlock(BlockState state) {
        if (state.isAir() || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.SNOW) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)) return false;
        SoundType sound = state.getSoundType();
        return sound != SoundType.GRAVEL && sound != SoundType.GRASS && sound != SoundType.SAND && sound != SoundType.SNOW && sound != SoundType.POWDER_SNOW && sound != SoundType.WOOL && sound != SoundType.SLIME_BLOCK && sound != SoundType.HONEY_BLOCK;
    }

@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) return;

        ItemStack stack = player.getItemInHand(hand);
        boolean isTamed = GoatExpansion.TAMED_GOATS.contains(this.getUUID());
        SimpleContainer inv = this.getGoatInventory();

        // 1. СТРАХОВКА: если коза уже приручена, клик по ней сразу выдаст достижение
        if (isTamed && player instanceof ServerPlayer sp) {
            triggerAdvancement(sp, "tame_goat");
        }

        // 2. Блок приручения новой дикой козы
        if (stack.is(Items.GOLDEN_CARROT) && !isTamed && !this.isBaby()) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (this.carrotsNeeded == 0) this.carrotsNeeded = 3 + RANDOM.nextInt(6);
            this.carrotsFed++;

            if (this.level() instanceof ServerLevel serverLevel) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GOAT_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                if (this.carrotsFed >= this.carrotsNeeded) {
                    GoatExpansion.TAMED_GOATS.add(this.getUUID());
                    serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.6, this.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
                    var maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        maxHealthAttr.setBaseValue(18.0);
                        this.setHealth(18.0f);
                    }

                    // Выдача достижения в момент приручения
                    if (player instanceof ServerPlayer sp) {
                        triggerAdvancement(sp, "tame_goat");
                    }
                } else {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // --- ДАЛЬШЕ ТВОЙ КОД ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ (float healAmount = ...) ---

        float healAmount = GoatExpansion.getHealAmount(stack);
        if (healAmount > 0.0F && this.getHealth() < this.getMaxHealth()) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (this.level() instanceof ServerLevel serverLevel) {
                this.heal(healAmount);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GOAT_EAT, SoundSource.NEUTRAL, 1.0f, 1.1f);
                serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.7, this.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (this.isBaby()) return;

        if (isTamed && (player.isCrouching() || stack.is(Items.CHEST))) {
            this.openCustomInventoryScreen(player);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (isTamed && stack.is(ModItems.IRON_HORN_CAP) && this.entityData.get(IRON_CAPS_DURABILITY) <= 0) {
            boolean hasNatL = this.entityData.get(DATA_HAS_LEFT_HORN);
            boolean hasNatR = this.entityData.get(DATA_HAS_RIGHT_HORN);
            if (hasNatL || hasNatR) {
                this.entityData.set(IRON_CAPS_DURABILITY, ModItems.CAP_MAX_DURABILITY);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }

        if (isTamed && stack.is(ModItems.IRON_PROSTHETIC_HORNS)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            String ownerUuid = customData != null ? customData.copyTag().getString("GoatOwnerUUID").orElse("") : "";

            if (!ownerUuid.equals(this.getUUID().toString())) {
                if (!this.level().isClientSide()) player.sendSystemMessage(Component.literal("§cЭтот протез выкован по меркам другой козы!"));
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            boolean hasNatL = this.entityData.get(DATA_HAS_LEFT_HORN);
            boolean hasNatR = this.entityData.get(DATA_HAS_RIGHT_HORN);
            int prosthL = this.entityData.get(PROSTHETIC_L_DURABILITY);
            int prosthR = this.entityData.get(PROSTHETIC_R_DURABILITY);

            if (!hasNatL && prosthL <= 0) {
                this.entityData.set(PROSTHETIC_L_DURABILITY, ModItems.PROSTHETIC_MAX_DURABILITY);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.NEUTRAL, 1.0f, 0.9f);
                if (player instanceof ServerPlayer sp) checkHornStateAdvancements(sp);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            } else if (!hasNatR && prosthR <= 0) {
                this.entityData.set(PROSTHETIC_R_DURABILITY, ModItems.PROSTHETIC_MAX_DURABILITY);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.NEUTRAL, 1.0f, 0.9f);
                if (player instanceof ServerPlayer sp) checkHornStateAdvancements(sp);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            } else {
                if (!this.level().isClientSide()) player.sendSystemMessage(Component.literal("§eОба рога (свои или протезы) уже на месте!"));
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }

        if (isTamed && stack.is(ModItems.GOAT_SADDLE) && inv.getItem(0).isEmpty()) {
            inv.setItem(0, stack.copyWithCount(1));
            this.setModSaddled(true);
            var maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) { maxHealthAttr.setBaseValue(28.0); this.heal(10.0F); }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0f, 1.0f);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (isTamed && this.goat_expansion$isSaddled() && stack.isEmpty() && !this.isVehicle() && this.goat_expansion$getStunTicks() <= 0) {
            if (!this.level().isClientSide()) player.startRiding(this);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
    }
}