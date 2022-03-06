package com.ericdebouwer.petdragon.enderdragonNMS;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.api.DragonSwoopEvent;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class PetEnderDragon_v1_17_R1_2 extends EnderDragon implements PetEnderDragon {

	static Field jumpField;
	static Method checkWalls;
	static Method checkCrystals;

	static {
		ResourceLocation mcKey = new ResourceLocation(PetEnderDragon.ENTITY_ID);
		try {
			if (!Registry.ENTITY_TYPE.getOptional(mcKey).isPresent()) {
				@SuppressWarnings("unchecked")
				Map<String, Type<?>> types = (Map<String, Type<?>>) DataFixers.getDataFixer().getSchema(
						DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getWorldVersion()))
						.findChoiceType(References.ENTITY).types();
				types.put(mcKey.toString(), types.get(Registry.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON).toString()));
				Registry.register(Registry.ENTITY_TYPE, PetEnderDragon.ENTITY_ID,
						EntityType.Builder.of(PetEnderDragon_v1_17_R1_2::new, MobCategory.MONSTER).noSummon().build(PetEnderDragon.ENTITY_ID));
			}

			// specialsource does ignore reflection, so non mapped names
			jumpField = LivingEntity.class.getDeclaredField("bn");
			jumpField.setAccessible(true);
			checkWalls = EnderDragon.class.getDeclaredMethod("b", AABB.class);
			checkWalls.setAccessible(true);
			checkCrystals = EnderDragon.class.getDeclaredMethod("fx");
			checkCrystals.setAccessible(true);
		} catch (NoSuchFieldException | NoSuchMethodException ignore) {
		}
	}

	private final PetDragon plugin;
	private long lastShot;
	private boolean didMove;
	int growlTicks = 100;

	public PetEnderDragon_v1_17_R1_2(EntityType<? extends EnderDragon> entitytypes, Level world) {
		this(world.getWorld());
	}

	public PetEnderDragon_v1_17_R1_2(World world) {
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
		this.level.addEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
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
	public boolean rideableUnderWater() { //ridable in water
		return true;
	}

	@Override
	public boolean save(CompoundTag nbttagcompound) {
		boolean result = super.save(nbttagcompound);
		nbttagcompound.putString("id", PetEnderDragon.ENTITY_ID);
		return result;
	}

	@Override
	public boolean hurt(EnderDragonPart entitycomplexpart, DamageSource damagesource, float f) {
		if (!(damagesource.getEntity() instanceof Player)) return false;
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
		} else {
			damagesource = DamageSource.explosion((LivingEntity) null); //fake explosion
			this.reallyHurt(damagesource, f);
			return true;
		}
	}
	
	@Override
	public boolean canChangeDimensions(){
		return true;
	}
	
    
	@Override
	// each movement update
	public void aiStep(){
		this.processFlappingMovement();
		if (this.level.isClientSide) {
			this.setHealth(this.getHealth());
			if (!this.isSilent() && --this.growlTicks < 0) { //noises & now also growl if stationary
				this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, this.getSoundSource(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
				this.growlTicks = 200 + this.random.nextInt(200);

			}
		}

		this.oFlapTime = this.flapTime; //flaptimes

		if (this.isDeadOrDying()) { //dead
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
			float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.level.addParticle(ParticleTypes.EXPLOSION, this.getX() + (double)f1, this.getY() + 2.0D + (double)f, this.getZ() + (double)f2, 0.0D, 0.0D, 0.0D);
		} else {
			try {
				checkCrystals.invoke(this); //crystals
			} catch (InvocationTargetException | IllegalAccessException ignore) {}

			this.flapTime += 0.1F;

			this.setYRot(Mth.wrapDegrees(this.getYRot()));

			// update position information
			if (this.posPointer < 0) {
				for(int i = 0; i < this.positions.length; ++i) {
					this.positions[i][0] = this.getYRot();
					this.positions[i][1] = this.getY();
				}
			}

			if (++this.posPointer == this.positions.length) {
				this.posPointer = 0;
			}

			this.positions[this.posPointer][0] = this.getYRot();
			this.positions[this.posPointer][1] = this.getY();
			this.yBodyRot = this.getYRot();

			//store parts locations
			Vec3[] avec3d = new Vec3[this.subEntities.length];

			for(int j = 0; j < this.subEntities.length; ++j) {
				avec3d[j] = new Vec3(this.subEntities[j].getX(), this.subEntities[j].getY(), this.subEntities[j].getZ());
			}

			float f7 = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * 0.017453292F;
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
			if (!this.level.isClientSide && this.hurtTime == 0) {

				this.knockBack(this.level.getEntities(this, this.subEntities[6].getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
				this.knockBack(this.level.getEntities(this, this.subEntities[7].getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
				this.hurt(this.level.getEntities(this, this.head.getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
				this.hurt(this.level.getEntities(this, this.subEntities[1].getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
			}

			float f13 = Mth.sin(this.getYRot() * 0.017453292F - this.yRotA * 0.01F);
			float f14 = Mth.cos(this.getYRot() * 0.017453292F - this.yRotA * 0.01F);
			float f15 = -1.0F;

			// update head and neck location
			this.subEntities[0].setPos(this.getX() + (double)(f13 * 6.5F * f8), this.getY() + (double)(f15 + f9 * 6.5F), this.getZ() + (double)(-f14 * 6.5F * f8));
			this.subEntities[1].setPos(this.getX() + (double)(f13 * 5.5F * f8), this.getY() + (double)(f15 + f9 * 5.5F), this.getZ() + (double)(-f14 * 5.5F * f8));
			double[] adouble = this.getLatencyPos(5, 1.0F);

			// move tail parts
			for(int k = 0; k < 3; ++k) {
				EnderDragonPart entitycomplexpart = this.subEntities[k + 3];

				double[] adouble1 = this.getLatencyPos(12 + k * 2, 1.0F);
				float f16 = (float) (this.getYRot() * 0.017453292F + Mth.wrapDegrees(adouble1[0] - adouble[0]) * 0.017453292F);
				float f3 = Mth.sin(f16);
				float f4 = Mth.cos(f16);
				float f17 = (float)(k + 1) * 2.0F;
				entitycomplexpart.setPos(this.getX() +(double)(-(f11 * 1.5F + f3 * f17) * f8),
						this.getY() + adouble1[1] - adouble[1] - (double)((f17 + 1.5F) * f9) + 1.5D,
						this.getZ() + (double)((f12 * 1.5F + f4 * f17) * f8));
			}

			if (!this.level.isClientSide && plugin.getConfigManager().isDoGriefing()) { //more efficient grieving check
				try {
					checkWalls.invoke(this, this.head.getBoundingBox());
					checkWalls.invoke(this, this.subEntities[1].getBoundingBox());
					checkWalls.invoke(this, this.subEntities[2].getBoundingBox());
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
		Player rider = (Player) this.getPassengers().get(0);

		Vector forwardDir = rider.getBukkitEntity().getLocation().getDirection();
		
		if (rider.getBukkitEntity().hasPermission("petdragon.shoot") && jumpField != null){
			try {
				boolean jumped = jumpField.getBoolean(rider);
				if (jumped && plugin.getConfigManager().getShootCooldown() * 1000 <= (System.currentTimeMillis() - lastShot)){

					Location loc = this.getBukkitEntity().getLocation();
					loc.add(forwardDir.clone().multiply(10).setY(-1));

					loc.getWorld().spawn(loc, DragonFireball.class, (fireball) -> {
						fireball.setDirection(forwardDir);
						fireball.setShooter(this.getEntity());
					});

					lastShot = System.currentTimeMillis();
				}
			} catch (IllegalArgumentException | IllegalAccessException ignore){
			}
		}

		this.setRot(180 + rider.getYRot(), rider.getXRot());
		this.setYHeadRot(rider.getXRot());
		
		double speeder = plugin.getConfigManager().getSpeedMultiplier();
		double fwSpeed = rider.zza * speeder;
		double sideSpeed = -1 * rider.xxa * speeder;
		
		Vector sideways = forwardDir.clone().crossProduct(new Vector(0,1,0));
    
		Vector total = forwardDir.multiply(fwSpeed).add(sideways.multiply(sideSpeed));
		this.move(MoverType.SELF, new Vec3(total.getX(), total.getY(), total.getZ()));

		// keep track of movement for wing hurting
		didMove = total.lengthSquared() > 0.1;
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
					entity.hurt(DamageSource.mobAttack(this), plugin.getConfigManager().getWingDamage());
					this.doEnchantDamageEffects(this, entity);
				}
			}
		}

	}

	// called for head
	private void hurt(List<Entity> list) {

		for (Entity entity : list) {
			if (entity instanceof LivingEntity) {
				entity.hurt(DamageSource.mobAttack(this), plugin.getConfigManager().getHeadDamage());
				this.doEnchantDamageEffects(this, entity);
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

			int viewDistance = (this.level).getCraftServer().getViewDistance() * 16;

			Iterator<ServerPlayer> var5 = this.level.getServer().getPlayerList().players.iterator();

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
				} while (this.level.spigotConfig.dragonDeathSoundRadius > 0
						&& distanceSquared > (double) (this.level.spigotConfig.dragonDeathSoundRadius * this.level.spigotConfig.dragonDeathSoundRadius));

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
			this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + (double)f, this.getY() + 2.0D + (double)f1, this.getZ() + (double)f2, 0.0D, 0.0D, 0.0D);

		}
		else {
			this.remove(RemovalReason.KILLED);
		}
		
	}

}
