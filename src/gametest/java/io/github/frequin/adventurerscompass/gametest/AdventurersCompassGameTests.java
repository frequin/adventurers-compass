package io.github.frequin.adventurerscompass.gametest;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

public class AdventurersCompassGameTests {

	@GameTest
	public void compassItemIsRegisteredInTheRunningGame(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(AdventurersCompassItem.KEY) == AdventurersCompass.ADVENTURERS_COMPASS_ITEM,
				"the Adventurer's Compass item should be registered");
		helper.succeed();
	}

}
