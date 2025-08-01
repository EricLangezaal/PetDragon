package com.ericdebouwer.petdragon.enderdragonNMS;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.api.DragonSwoopEvent;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftLivingEntity;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PetEnderDragon_v1_21_R3 extends EnderDragon implements PetEnderDragon {

    static Field jumpField;
    static Method checkWalls;
    static Method checkCrystals;

    static {
        ResourceLocation mcKey = ResourceLocation.parse(PetEnderDragon.ENTITY_ID);
        try {
            if (!BuiltInRegistries.ENTITY_TYPE.getOptional(mcKey).isPresent()){
                injectEntity(mcKey);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex){
            JavaPlugin.getPlugin(PetDragon.class).getLogger().log(java.util.logging.Level.WARNING, "Failed to inject custom entity! " +
                    "The plugin will still work, but might be slightly less efficient and dragons might not persist.", ex);

        }
        finally {
            try {
                checkWalls = ReflectionUtils.getMethod(EnderDragon.class, boolean.class, ServerLevel.class, AABB.class);
                checkCrystals = ReflectionUtils.getMethod("gr", EnderDragon.class);
            } catch (NoSuchMethodException ignore) {
            }
        }
    }

    private static void injectEntity(ResourceLocation mcKey) throws NoSuchFieldException, IllegalAccessException {
        Registry<EntityType<?>> entityReg = ((CraftServer)Bukkit.getServer()).getServer()
                .registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

        ReflectionUtils.setField("m", MappedRegistry.class, entityReg, new IdentityHashMap<EntityType<?>, Holder.Reference<EntityType<?>>>());
        // frozen = False (only boolean)
        ReflectionUtils.setField(MappedRegistry.class, entityReg, false);

        try {
            // Paper wants this, Spigot this causes a crash
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            @SuppressWarnings("unchecked")
            Map<String, Type<?>> types = (Map<String, Type<?>>) DataFixers.getDataFixer().getSchema(
                            DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))
                    .findChoiceType(References.ENTITY).types();
            types.put(mcKey.toString(), types.get(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON).toString()));
        } catch (ClassNotFoundException ignored){}

        EntityType<?> builtEnt = EntityType.Builder.of(PetEnderDragon_v1_21_R3::new, EntityType.ENDER_DRAGON.getCategory())
                .noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, mcKey));
        entityReg.createIntrusiveHolder(builtEnt);
        Registry.register(entityReg, PetEnderDragon.ENTITY_ID, builtEnt);
    }

    private final PetDragon plugin;
    private long lastShot;
    private boolean didMove;
    int growlTicks = 100;

    public PetEnderDragon_v1_21_R3(EntityType<? extends EnderDragon> entitytypes, Level world) {
        this(world.getWorld());
    }

    public PetEnderDragon_v1_21_R3(World world) {
        super(EntityType.ENDER_DRAGON, ((CraftWorld)world).getHandle());
        this.plugin = JavaPlugin.getPlugin(PetDragon.class);
        this.setupDefault();
        this.getBukkitEntity().setSilent(plugin.getConfigManager().isSilent());
        this.noPhysics = plugin.getConfigManager().isFlyThroughBlocks();
    }

    @Override
    public void copyFrom(org.bukkit.entity.EnderDragon dragon) {
        EnderDragon other = ((CraftEnderDragon) dragon).getHandle();
        CompoundTag nbt = other.saveWithoutId(new CompoundTag());
        nbt.remove("Passengers");
        nbt.remove("WorldUUIDLeast");
        nbt.remove("WorldUUIDMost");
        nbt.remove("UUID"); // probably not required for this version
        nbt.putBoolean("Silent", plugin.getConfigManager().isSilent());
        this.load(nbt);
    }

    @Override
    public void spawn(Vector location) {
        this.setPos(location.getX(), location.getY(), location.getZ());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    @Override
    public org.bukkit.entity.EnderDragon getEntity() {
        return (org.bukkit.entity.EnderDragon) this.getBukkitEntity();
    }

    @Override
    protected boolean isAffectedByFluids() { //affected by fluids
        return false;
    }


    @Override
    public boolean dismountsUnderwater() { //ridable in water
        return true;
    }

    @Override
    public boolean save(CompoundTag nbttagcompound) {
        boolean result = super.save(nbttagcompound);
        nbttagcompound.putString("id", PetEnderDragon.ENTITY_ID);
        return result;
    }

    @Override
    public boolean hurt(ServerLevel worldserver, EnderDragonPart entitycomplexpart, DamageSource damagesource, float f) {
        if (!(damagesource.getEntity() instanceof Player)) return false;
        if (damagesource.is(DamageTypes.THORNS)) return false;
        HumanEntity damager = (HumanEntity) damagesource.getEntity().getBukkitEntity();

        if (plugin.getConfigManager().isLeftClickRide()){
            if (plugin.getFactory().tryRide(damager, (org.bukkit.entity.EnderDragon) this.getBukkitEntity())){
                return false; //cancel damage
            }

        }

        if (!plugin.getFactory().canDamage(damager, this)) return false;

        f = getPhaseManager().getCurrentPhase().onHurt(damagesource, f);
        f = f / (200.0F / MAX_HEALTH);

        //head 4x as much damage
        if (entitycomplexpart != this.head) {
            f = f / 4.0F + Math.min(f, 1.0F);
        }
        if (f < 0.01F) {
            return false;
        }
        this.reallyHurt(worldserver, this.damageSources().generic(), f);
        return true;
    }

    @Override
    public boolean canUsePortal(boolean flag){
        return true;
    }

    @Override
    // each movement update
    public void aiStep(){
        this.processFlappingMovement();
        if (this.level().isClientSide) {
            this.setHealth(this.getHealth());
            if (!this.isSilent() && --this.growlTicks < 0) { //noises & now also growl if stationary
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, this.getSoundSource(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
                this.growlTicks = 200 + this.random.nextInt(200);

            }
        }

        this.oFlapTime = this.flapTime; //flaptimes

        if (this.isDeadOrDying()) { //dead
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
            float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level().addParticle(ParticleTypes.EXPLOSION, this.getX() + (double)f1, this.getY() + 2.0D + (double)f, this.getZ() + (double)f2, 0.0D, 0.0D, 0.0D);
        } else {
            try {
                checkCrystals.invoke(this); //crystals
            } catch (InvocationTargetException | IllegalAccessException ignore) {}

            this.flapTime += 0.1F;
            this.setYRot(Mth.wrapDegrees(this.getYRot()));

            this.flightHistory.record(this.getY(), this.getYRot());
            this.applyEffectsFromBlocks();

            this.yBodyRot = this.getYRot();

            //store parts locations
            Vec3[] avec3d = new Vec3[this.subEntities.length];

            for(int j = 0; j < this.subEntities.length; ++j) {
                avec3d[j] = new Vec3(this.subEntities[j].getX(), this.subEntities[j].getY(), this.subEntities[j].getZ());
            }

            float f7 = (float)(this.flightHistory.get(5).y() - this.flightHistory.get(10).y()) * 10.0F * 0.017453292F;
            float f8 = Mth.cos(f7);
            float f9 = Mth.sin(f7);
            float f6 = this.getYRot() * 0.017453292F;
            float f11 = Mth.sin(f6);
            float f12 = Mth.cos(f6);

            // move body and wings
            this.subEntities[2].setPos(this.getX() + (double)(f11 * 0.5F) , this.getY(), this.getZ() + (double)(-f12 * 0.5F));
            this.subEntities[6].setPos(this.getX() + (double)(f12 * 4.5F) , this.getY() + 2.0D, this.getZ() + (double)(f11 * 4.5F));
            this.subEntities[7].setPos(this.getX() + (double)(f12 * -4.5F) , this.getY() + 2.0D, this.getZ() + (double)(f11 * -4.5F));

            // do knockback, wing hurt and head/neck attack
            if (!this.level().isClientSide && this.hurtTime == 0) {

                this.knockBack(this.level().getEntities(this, this.subEntities[6].getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                this.knockBack(this.level().getEntities(this, this.subEntities[7].getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                this.hurt(this.level().getEntities(this, this.head.getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                this.hurt(this.level().getEntities(this, this.subEntities[1].getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            }

            float f13 = Mth.sin(this.getYRot() * 0.017453292F - this.yRotA * 0.01F);
            float f14 = Mth.cos(this.getYRot() * 0.017453292F - this.yRotA * 0.01F);
            float f15 = -1.0F;

            // update head and neck location
            this.subEntities[0].setPos(this.getX() + (double)(f13 * 6.5F * f8), this.getY() + (double)(f15 + f9 * 6.5F), this.getZ() + (double)(-f14 * 6.5F * f8));
            this.subEntities[1].setPos(this.getX() + (double)(f13 * 5.5F * f8), this.getY() + (double)(f15 + f9 * 5.5F), this.getZ() + (double)(-f14 * 5.5F * f8));
            DragonFlightHistory.Sample hist1 = this.flightHistory.get(5);

            // move tail parts
            for(int k = 0; k < 3; ++k) {
                EnderDragonPart entitycomplexpart = this.subEntities[k + 3];

                DragonFlightHistory.Sample hist2 = this.flightHistory.get(12 + k * 2);
                float f16 = (float) (this.getYRot() * 0.017453292F + Mth.wrapDegrees(hist2.yRot() - hist1.yRot()) * 0.017453292F);
                float f3 = Mth.sin(f16);
                float f4 = Mth.cos(f16);
                float f17 = (float)(k + 1) * 2.0F;
                entitycomplexpart.setPos(this.getX() +(double)(-(f11 * 1.5F + f3 * f17) * f8),
                        this.getY() + hist2.y() - hist1.y() - (double)((f17 + 1.5F) * f9) + 1.5D,
                        this.getZ() + (double)((f12 * 1.5F + f4 * f17) * f8));
            }

            if (!this.level().isClientSide && plugin.getConfigManager().isDoGriefing()) { //more efficient grieving check
                try {
                    checkWalls.invoke(this, this.level(), this.head.getBoundingBox());
                    checkWalls.invoke(this, this.level(), this.subEntities[1].getBoundingBox());
                    checkWalls.invoke(this, this.level(), this.subEntities[2].getBoundingBox());
                } catch (IllegalAccessException | InvocationTargetException ignore){}
            }

            // update positions of children
            for(int k = 0; k < this.subEntities.length; ++k) {
                this.subEntities[k].xo = avec3d[k].x;
                this.subEntities[k].yo = avec3d[k].y;
                this.subEntities[k].zo = avec3d[k].z;
                this.subEntities[k].xOld = avec3d[k].x;
                this.subEntities[k].yOld = avec3d[k].y;
                this.subEntities[k].zOld = avec3d[k].z;
            }
        }

        if (this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof Player)){
            didMove = false;
            return;
        }

        ServerPlayer rider = (ServerPlayer) this.getPassengers().get(0);
        Vector forwardDir = rider.getBukkitEntity().getLocation().getDirection();

        if (
            rider.getBukkitEntity().hasPermission("petdragon.shoot") &&
            rider.getLastClientInput().jump() &&
            plugin.getConfigManager().getShootCooldown() * 1000 <= (System.currentTimeMillis() - lastShot)
        ) {
            Location loc = this.getBukkitEntity().getLocation();
            loc.add(forwardDir.clone().multiply(10).setY(-1));

            loc.getWorld().spawn(loc, DragonFireball.class, (fireball) -> {
                fireball.setDirection(forwardDir);
                fireball.setShooter(this.getEntity());
            });

            lastShot = System.currentTimeMillis();
        }

        this.setRot(180 + rider.getYRot(), rider.getXRot());
        this.setYHeadRot(rider.getXRot());

        Vec3 movementNoY = rider.getLastClientMoveIntent().scale(plugin.getConfigManager().getSpeedMultiplier());
        Vec3 forwardOnlyMovement = movementNoY.projectedOn(rider.getLookAngle());

        Vec3 totalMovement = movementNoY.with(Direction.Axis.Y, forwardOnlyMovement.y());
        this.move(MoverType.SELF, totalMovement);
        didMove = totalMovement.lengthSqr() > 0.1;
    }

    // called for wings
    private void knockBack(List<Entity> list) {
        double midBodyX = (this.subEntities[2].getBoundingBox().minX + this.subEntities[2].getBoundingBox().maxX) / 2.0D;
        double midBodyZ = (this.subEntities[2].getBoundingBox().minZ + this.subEntities[2].getBoundingBox().maxZ) / 2.0D;

        for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
                double disX = entity.getX() - midBodyX;
                double disZ = entity.getZ() - midBodyZ;
                double totalDis = Math.max(disX * disX + disZ * disZ, 0.1D);

                DragonSwoopEvent swoopEvent = new DragonSwoopEvent(this.getEntity(), (org.bukkit.entity.LivingEntity) entity.getBukkitEntity(),
                        new Vector(disX / totalDis * 4.0D,  0.20000000298023224D, disZ / totalDis * 4.0D));
                swoopEvent.setCancelled(!plugin.getConfigManager().isInteractEntities());
                Bukkit.getPluginManager().callEvent(swoopEvent);

                if (!swoopEvent.isCancelled() && swoopEvent.getTarget() != null){
                    LivingEntity nmsEntity = ((CraftLivingEntity) swoopEvent.getTarget()).getHandle();
                    nmsEntity.push(swoopEvent.getVelocity().getX(), swoopEvent.getVelocity().getY(), swoopEvent.getVelocity().getZ());
                }

                if (didMove && ((LivingEntity) entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
                    DamageSource damagesource = this.damageSources().mobAttack(this);
                    entity.hurt(damagesource, plugin.getConfigManager().getWingDamage());
                    EnchantmentHelper.doPostAttackEffects((ServerLevel) this.level(), entity, damagesource);
                }
            }
        }

    }

    // called for head
    private void hurt(List<Entity> list) {
        for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
                DamageSource damagesource = this.damageSources().mobAttack(this);
                entity.hurt(damagesource, plugin.getConfigManager().getHeadDamage());
                EnchantmentHelper.doPostAttackEffects((ServerLevel) this.level(), entity, damagesource);
            }
        }
    }


    @Override
    public void tickDeath(){
        ++this.dragonDeathTime;

        if (!plugin.getConfigManager().isDeathAnimation()){
            this.remove(RemovalReason.KILLED);
            return;
        }
        // make players nearby aware of his death

        if (this.dragonDeathTime == 1 && !this.isSilent()) {

            int viewDistance = (this.level()).getCraftServer().getViewDistance() * 16;

            Iterator<ServerPlayer> var5 = this.level().getServer().getPlayerList().players.iterator();

            label59 : while (true) {
                ServerPlayer player;
                double deltaX;
                double deltaZ;
                double distanceSquared;
                do {
                    if (!var5.hasNext()) {
                        break label59;
                    }

                    player = var5.next();
                    deltaX = this.getX() - player.getX();
                    deltaZ = this.getZ() - player.getZ();
                    distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                } while (this.level().spigotConfig.dragonDeathSoundRadius > 0
                        && distanceSquared > (double) (this.level().spigotConfig.dragonDeathSoundRadius * this.level().spigotConfig.dragonDeathSoundRadius));

                if (distanceSquared > (double) (viewDistance * viewDistance)) {
                    double deltaLength = Math.sqrt(distanceSquared);
                    double relativeX = player.getX() + deltaX / deltaLength
                            * (double) viewDistance;
                    double relativeZ = player.getZ() + deltaZ / deltaLength
                            * (double) viewDistance;
                    player.connection.send(new ClientboundLevelEventPacket(1028, new BlockPos((int)relativeX, (int)this.getY(), (int)relativeZ), 0, true));

                } else {
                    player.connection.send(new ClientboundLevelEventPacket(1028, new BlockPos((int)this.getX(), (int)this.getY(), (int)this.getZ()), 0, true));

                }
            }
        }


        if (this.dragonDeathTime <= 100) {
            // particle stuff
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
            float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + (double)f, this.getY() + 2.0D + (double)f1, this.getZ() + (double)f2, 0.0D, 0.0D, 0.0D);

        }
        else {
            this.remove(RemovalReason.KILLED);
        }

    }
}
