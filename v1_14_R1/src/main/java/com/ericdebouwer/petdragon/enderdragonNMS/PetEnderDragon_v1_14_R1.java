package com.ericdebouwer.petdragon.enderdragonNMS;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.api.DragonSwoopEvent;
import net.minecraft.server.v1_14_R1.AxisAlignedBB;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.DamageSource;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityComplexPart;
import net.minecraft.server.v1_14_R1.EntityEnderDragon;
import net.minecraft.server.v1_14_R1.EntityHuman;
import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumMoveType;
import net.minecraft.server.v1_14_R1.FluidType;
import net.minecraft.server.v1_14_R1.IEntitySelector;
import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.PacketPlayOutWorldEvent;
import net.minecraft.server.v1_14_R1.Particles;
import net.minecraft.server.v1_14_R1.SoundEffects;
import net.minecraft.server.v1_14_R1.Tag;
import net.minecraft.server.v1_14_R1.Vec3D;
import net.minecraft.server.v1_14_R1.World;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class PetEnderDragon_v1_14_R1 extends EntityEnderDragon  implements PetEnderDragon {

	private PetDragon plugin;
	Location loc;

	static Field jumpField;
	static Method checkWalls;
	static Method checkCrystals;
	static {
		try {
			jumpField = EntityLiving.class.getDeclaredField("jumping");
			jumpField.setAccessible(true);
			checkWalls = EntityEnderDragon.class.getDeclaredMethod("b", AxisAlignedBB.class);
			checkWalls.setAccessible(true);
			checkCrystals = EntityEnderDragon.class.getDeclaredMethod("dW");
			checkCrystals.setAccessible(true);
		} catch (NoSuchFieldException | NoSuchMethodException ignore) {
		}
	}

	private long lastShot;
	private boolean didMove;
	int growlTicks = 100;

	public PetEnderDragon_v1_14_R1(EntityTypes<? extends EntityEnderDragon> entitytypes, World world) {
		super(EntityTypes.ENDER_DRAGON, world);
	}
	
	public PetEnderDragon_v1_14_R1(Location loc, PetDragon plugin){
		super(null, ((CraftWorld)loc.getWorld()).getHandle());
		this.plugin = plugin;
		this.loc = loc;
		
		this.setupDefault();
		this.getBukkitEntity().setSilent(plugin.getConfigManager().isSilent());
		this.noclip = plugin.getConfigManager().isFlyThroughBlocks();
		
		this.setPosition(loc.getX(), loc.getY(), loc.getZ());
		
	}
	
	@Override
	public void copyFrom(EnderDragon dragon) {
		EntityEnderDragon other = ((CraftEnderDragon) dragon).getHandle();
		NBTTagCompound nbt = other.save(new NBTTagCompound());
		nbt.remove("Passengers");
		nbt.remove("WorldUUIDLeast");
		nbt.remove("WorldUUIDMost");
		nbt.remove("UUID");
		nbt.setBoolean("Silent", plugin.getConfigManager().isSilent());
		this.f(nbt);
	}
	
	
	public void spawn(){
		((CraftWorld)loc.getWorld()).getHandle().addEntity(this, SpawnReason.CUSTOM);
	}
	
	@Override
	public EnderDragon getEntity() {
		return (EnderDragon) this.getBukkitEntity();
	}
	
	@Override
	protected void c(Tag<FluidType> tag) { //handle in water (do nothing so it wont change)
	};
	
	
	@Override
	public boolean bf() { //ridable in water
		return true;
	};
		
	
	@Override
	public boolean a(EntityComplexPart entitycomplexpart, DamageSource damagesource, float f) {
		if (!(damagesource.getEntity() instanceof EntityHuman)) return false;
		HumanEntity damager = (HumanEntity) damagesource.getEntity().getBukkitEntity();
		
		if (plugin.getConfigManager().isLeftClickRide()){
			if (plugin.getFactory().tryRide(damager, (EnderDragon) this.getBukkitEntity())){
				return false; //cancel damage
			}
			
		}
		
		if (!plugin.getFactory().canDamage(damager, this)) return false;
		
		f = getDragonControllerManager().a().a(damagesource, f);
		f = f / (200.0F / MAX_HEALTH);
		
		//head 4x as much damage
		if (entitycomplexpart != this.bA) {
			f = f / 4.0F + Math.min(f, 1.0F);
		}
		
		if (f < 0.01F) {
			return false;
		} else {
			damagesource = DamageSource.b(null); //fake explosion
			this.dealDamage(damagesource, f);
			
			return true;
		}
	}
	
	@Override
	public boolean canPortal(){
		return true;
	}
	
    
	@Override
	// each movement update
	public void movementTick(){

		if (this.world.isClientSide) {
			this.setHealth(this.getHealth());
			if (!this.isSilent()) { //noises
				float f = MathHelper.cos(this.bJ * 6.2831855F);
				float f1 = MathHelper.cos(this.bI * 6.2831855F);
				if (f1 <= -0.3F && f >= -0.3F) {
					this.world.a(this.locX, this.locY, this.locZ, SoundEffects.ENTITY_ENDER_DRAGON_FLAP, this.getSoundCategory(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
				}

				if (--this.growlTicks < 0) { //now also growl if stationary
					this.world.a(this.locX, this.locY, this.locZ, SoundEffects.ENTITY_ENDER_DRAGON_GROWL, this.getSoundCategory(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
					this.growlTicks = 200 + this.random.nextInt(200);
				}
			}
		}

		this.bI = this.bJ; //flaptimes

		if (this.getHealth() <= 0.0F) { //dead
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
			float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.world.addParticle(Particles.EXPLOSION, this.locX + (double)f, this.locY + 2.0D + (double)f1, this.locZ + (double)f2, 0.0D, 0.0D, 0.0D);
		} else {
			try {
				checkCrystals.invoke(this); //crystals
			} catch (InvocationTargetException | IllegalAccessException ignore) {}

			this.bJ += 0.1F;

			this.yaw = MathHelper.g(this.yaw);

			// update position information
			if (this.d < 0) {
				for(int i = 0; i < this.c.length; ++i) {
					this.c[i][0] = this.yaw;
					this.c[i][1] = this.locY;
				}
			}

			if (++this.d == this.c.length) {
				this.d = 0;
			}

			this.c[this.d][0] = this.yaw;
			this.c[this.d][1] = this.locY;
			this.aK = this.yaw;

			//store parts locations
			Vec3D[] avec3d = new Vec3D[this.children.length];

			for(int j = 0; j < this.children.length; ++j) {
				avec3d[j] = new Vec3D(this.children[j].locX, this.children[j].locY, this.children[j].locZ);
			}

			float f7 = (float)(this.a(5, 1.0F)[1] - this.a(10, 1.0F)[1]) * 10.0F * 0.017453292F;
			float f8 = MathHelper.cos(f7);
			float f9 = MathHelper.sin(f7);
			float f10 = this.yaw * 0.017453292F;
			float f11 = MathHelper.sin(f10);
			float f12 = MathHelper.cos(f10);

			// move body and wings
			this.bC.tick();
			this.bC.setPositionRotation(this.locX + (double)(f11 * 0.5F), this.locY, this.locZ - (double)(f12 * 0.5F), 0.0F, 0.0F);
			this.bG.tick();
			this.bG.setPositionRotation(this.locX + (double)(f12 * 4.5F), this.locY + 2.0D, this.locZ + (double)(f11 * 4.5F), 0.0F, 0.0F);
			this.bH.tick();
			this.bH.setPositionRotation(this.locX - (double)(f12 * 4.5F), this.locY + 2.0D, this.locZ - (double)(f11 * 4.5F), 0.0F, 0.0F);

			// do knockback, wing hurt and head/neck attack
			if (!this.world.isClientSide && this.hurtTicks == 0) {

				this.knockBack(this.world.getEntities(this, this.children[6].getBoundingBox().grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D), IEntitySelector.e));
				this.knockBack(this.world.getEntities(this, this.children[7].getBoundingBox().grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D), IEntitySelector.e));
				this.hurt(this.world.getEntities(this, this.bA.getBoundingBox().g(1.0D), IEntitySelector.e));
				this.hurt(this.world.getEntities(this, this.children[1].getBoundingBox().g(1.0D), IEntitySelector.e));
			}

			double[] adouble = this.a(5, 1.0F);
			float f13 = MathHelper.sin(this.yaw * 0.017453292F - this.be * 0.01F);
			float f14 = MathHelper.cos(this.yaw * 0.017453292F - this.be * 0.01F);
			this.bA.tick();
			this.bB.tick();
			float f3 = -1.0F;
			this.bA.setPositionRotation(this.locX + (double)(f13 * 6.5F * f8), this.locY + (double)f3 + (double)(f9 * 6.5F), this.locZ - (double)(f14 * 6.5F * f8), 0.0F, 0.0F);
			this.bB.setPositionRotation(this.locX + (double)(f13 * 5.5F * f8), this.locY + (double)f3 + (double)(f9 * 5.5F), this.locZ - (double)(f14 * 5.5F * f8), 0.0F, 0.0F);

			// move tail parts
			for(int k = 0; k < 3; ++k) {
				EntityComplexPart entitycomplexpart = this.children[k + 3];

				double[] adouble1 = this.a(12 + k * 2, 1.0F);
				float f15 = this.yaw * 0.017453292F + (float) MathHelper.g(adouble1[0] - adouble[0]) * 0.017453292F;
				float f16 = MathHelper.sin(f15);
				float f17 = MathHelper.cos(f15);
				float f19 = (float)(k + 1) * 2.0F;
				entitycomplexpart.tick();
				entitycomplexpart.setPositionRotation(this.locX - (double)((f11 * 1.5F + f16 * f19) * f8), this.locY + (adouble1[1] - adouble[1]) - (double)((f19 + 1.5F) * f9) + 1.5D, this.locZ + (double)((f12 * 1.5F + f17 * f19) * f8), 0.0F, 0.0F);
			}

			if (!this.world.isClientSide && plugin.getConfigManager().isDoGriefing()) { //more efficient grieving check
				try {
					checkWalls.invoke(this, this.bA.getBoundingBox());
					checkWalls.invoke(this, this.children[1].getBoundingBox());
					checkWalls.invoke(this, this.children[2].getBoundingBox());
				} catch (IllegalAccessException | InvocationTargetException ignore){}
			}

			// update positions of children
			for(int k = 0; k < this.children.length; ++k) {
				this.children[k].lastX = avec3d[k].x;
				this.children[k].lastY = avec3d[k].y;
				this.children[k].lastZ = avec3d[k].z;
			}
		}
		
		if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof EntityHuman)){
			this.didMove = false;
			return;
		}
		EntityHuman rider = (EntityHuman) this.passengers.get(0);
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
		
		this.setYawPitch(180 + rider.yaw, rider.pitch);
		this.setHeadRotation(rider.pitch);
		
		double speeder = plugin.getConfigManager().getSpeedMultiplier();
		double fwSpeed = rider.bd * speeder;
		double sideSpeed = -1 * rider.bb * speeder;
		
		Vector sideways = forwardDir.clone().crossProduct(new Vector(0,1,0));

		Vector total = forwardDir.multiply(fwSpeed).add(sideways.multiply(sideSpeed));
		this.move(EnumMoveType.SELF, new Vec3D(total.getX(), total.getY(), total.getZ()));

		// keep track of movement for wing hurting
		didMove = total.lengthSquared() > 0.1;
	}

	// called for wings
	private void knockBack(List<Entity> list) {
		double midBodyX = (this.children[2].getBoundingBox().minX + this.children[2].getBoundingBox().maxX) / 2.0D;
		double midBodyZ = (this.children[2].getBoundingBox().minZ + this.children[2].getBoundingBox().maxZ) / 2.0D;

		for (Entity entity : list) {
			if (entity instanceof EntityLiving) {
				double disX = entity.locX - midBodyX;
				double disZ = entity.locZ - midBodyZ;
				double totalDis = Math.max(disX * disX + disZ * disZ, 0.1D);

				DragonSwoopEvent swoopEvent = new DragonSwoopEvent(this.getEntity(), (LivingEntity) entity.getBukkitEntity(),
						new Vector(disX / totalDis * 4.0D,  0.20000000298023224D, disZ / totalDis * 4.0D));
				swoopEvent.setCancelled(!plugin.getConfigManager().isInteractEntities());
				Bukkit.getPluginManager().callEvent(swoopEvent);

				if (!swoopEvent.isCancelled() && swoopEvent.getTarget() != null){
					EntityLiving nmsEntity = ((CraftLivingEntity) swoopEvent.getTarget()).getHandle();
					nmsEntity.f(swoopEvent.getVelocity().getX(), swoopEvent.getVelocity().getY(), swoopEvent.getVelocity().getZ());
				}

				if (didMove && ((EntityLiving) entity).ct() < entity.ticksLived - 2) {
					entity.damageEntity(DamageSource.mobAttack(this), plugin.getConfigManager().getWingDamage());
					this.a(this, entity);
				}
			}
		}

	}

	// called for head
	private void hurt(List<Entity> list) {
		for (Entity entity : list) {
			if (entity instanceof EntityLiving) {
				entity.damageEntity(DamageSource.mobAttack(this), plugin.getConfigManager().getHeadDamage());
				this.a(this, entity);
			}
		}
	}



	@Override
	public void co(){
		++this.bL;
		
		if (!plugin.getConfigManager().isDeathAnimation()){
			this.die();
			return;
		}
		
		// make players nearby aware of his death 
			
		if (this.bL == 1 && !this.isSilent()) {
			int viewDistance = ((WorldServer) this.world).getServer()
					.getViewDistance() * 16;
			@SuppressWarnings("deprecation")
			Iterator<EntityPlayer> var5 = MinecraftServer.getServer().getPlayerList().players
					.iterator();

			label59 : while (true) {
				EntityPlayer player;
				double deltaX;
				double deltaZ;
				double distanceSquared;
				do {
					if (!var5.hasNext()) {
						break label59;
					}

					player = (EntityPlayer) var5.next();
					deltaX = this.locX - player.locX;
					deltaZ = this.locZ - player.locZ;
					distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
				} while (this.world.spigotConfig.dragonDeathSoundRadius > 0
						&& distanceSquared > (double) (this.world.spigotConfig.dragonDeathSoundRadius * this.world.spigotConfig.dragonDeathSoundRadius));

				if (distanceSquared > (double) (viewDistance * viewDistance)) {
					double deltaLength = Math.sqrt(distanceSquared);
					double relativeX = player.locX + deltaX / deltaLength
							* (double) viewDistance;
					double relativeZ = player.locZ + deltaZ / deltaLength
							* (double) viewDistance;
					player.playerConnection
							.sendPacket(new PacketPlayOutWorldEvent(1028,
									new BlockPosition((int) relativeX,
											(int) this.locY,
											(int) relativeZ), 0, true));
				} else {
					player.playerConnection
							.sendPacket(new PacketPlayOutWorldEvent(1028,
									new BlockPosition((int) this.locX,
											(int) this.locY, (int) this
													.locZ), 0, true));
				}
			}
		}
		
		
		if (this.bL <= 100) {
			// particle stuff
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
			float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.world.addParticle(Particles.EXPLOSION_EMITTER, this.locX
					+ (double) f, this.locY + 2.0D + (double) f1, this.locZ
					+ (double) f2, 0.0D, 0.0D, 0.0D);
		}
		else {
			this.die();
		}
		
	}


}
