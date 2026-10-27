package io.github.frequin.adventurerscompass.util;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class AdventurersCompassAngle implements RangeSelectItemModelProperty {
	
	public static final MapCodec<AdventurersCompassAngle> MAP_CODEC = MapCodec.unit(new AdventurersCompassAngle());
	private final AdventurersCompassAngleState state;
	
	public AdventurersCompassAngle() {
		this(new AdventurersCompassAngleState());
	}
	
	private AdventurersCompassAngle(AdventurersCompassAngleState state) {
		this.state = state;
	}
	
	@Override
	public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		return state.get(stack, level, owner, seed);
	}
	
	@Override
	public MapCodec<AdventurersCompassAngle> type() {
		return MAP_CODEC;
	}
	
}