package io.github.frequin.adventurerscompass.util;

import org.jetbrains.annotations.Nullable;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class AdventurersCompassAngleState extends NeedleDirectionHelper {
	
	private final NeedleDirectionHelper.Wobbler wobbler;
	private final RandomSource random = RandomSource.create();
	
	public AdventurersCompassAngleState() {
		super(true);
		wobbler = newWobbler(0.8F);
	}
	
	/**
	 * Points at the Target in the Current dimension. Spins while searching and
	 * when the Target is in another dimension; points at the world spawn when
	 * the compass has no Target.
	 */
	@Override
	protected float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner) {
		long gameTime = level.getGameTime();
		GlobalPos pos = level.getRespawnData().globalPos();
		if (stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM) {
			CompassState state = CompassStacks.read(stack);
			String dimension = level.dimension().identifier().toString();
			boolean targetHere = state.target() != null && state.target().dimension().equals(dimension);
			if (state.isSearching() || state.target() != null && !targetHere) {
				return getRandomlySpinningRotation(seed, gameTime);
			}
			// A kept Target is pointed at even while the HUD shows "nothing found"
			if (targetHere) {
				pos = new GlobalPos(level.dimension(), new BlockPos(state.target().pos().x(), 0, state.target().pos().z()));
			}
		}
		return owner.asLivingEntity() == null || !isValidCompassTargetPos(owner.asLivingEntity(), pos) ? getRandomlySpinningRotation(seed, gameTime) : getRotationTowardsCompassTarget(owner.asLivingEntity(), gameTime, pos.pos());
	}

	private float getRandomlySpinningRotation(int seed, long gameTime) {
		if (wobbler.shouldUpdate(gameTime)) {
			wobbler.update(gameTime, random.nextFloat());
		}
		
		float f = wobbler.rotation() + (float) hash(seed) / 2.1474836E9F;
		return Mth.positiveModulo(f, 1.0F);
	}
	
	private float getRotationTowardsCompassTarget(Entity entity, long gameTime, BlockPos pos) {
		float f = (float) getAngleFromEntityToPos(entity, pos);
		float f1 = getWrappedVisualRotationY(entity);
		if (entity instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
			if (wobbler.shouldUpdate(gameTime)) {
				wobbler.update(gameTime, 0.5F - (f1 - 0.25F));
			}
			
			float f3 = f + wobbler.rotation();
			return Mth.positiveModulo(f3, 1.0F);
		}
		
		float f2 = 0.5F - (f1 - 0.25F - f);
		return Mth.positiveModulo(f2, 1.0F);
	}
	
	private static boolean isValidCompassTargetPos(Entity entity, @Nullable GlobalPos pos) {
		return pos != null && pos.dimension() == entity.level().dimension() && !(pos.pos().distToCenterSqr(entity.position()) < 1.0E-5F);
	}
	
	private static double getAngleFromEntityToPos(Entity entity, BlockPos pos) {
		Vec3 vec3 = Vec3.atCenterOf(pos);
		return Math.atan2(vec3.z() - entity.getZ(), vec3.x() - entity.getX()) / (float) (Math.PI * 2);
	}
	
	private static float getWrappedVisualRotationY(Entity entity) {
		return Mth.positiveModulo(entity.getVisualRotationYInDegrees() / 360.0F, 1.0F);
	}
	
	private static int hash(int seed) {
		return seed * 1327217883;
	}
	
}