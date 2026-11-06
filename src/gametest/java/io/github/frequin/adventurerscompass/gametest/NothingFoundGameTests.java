package io.github.frequin.adventurerscompass.gametest;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.domain.Clock;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** The "nothing found" of a real compass item, as ticked in a player's inventory. */
public class NothingFoundGameTests {

	private static final SearchResult.NothingFound NOTHING = new SearchResult.NothingFound("minecraft:overworld", 1000, 0, 0);

	@GameTest
	public void aRecentMessageStaysOnACompassInEitherHand(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack main = showingNothingFound();
		ItemStack off = showingNothingFound();

		main.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		off.inventoryTick(helper.getLevel(), player, EquipmentSlot.OFFHAND);

		helper.assertValueEqual(NOTHING, CompassStacks.read(main).nothingFound(), "main hand message");
		helper.assertValueEqual(NOTHING, CompassStacks.read(off).nothingFound(), "off hand message");
		helper.succeed();
	}

	@GameTest
	public void aCompassPutAwayLosesItsMessage(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack stack = showingNothingFound();

		// Vanilla ticks a stack outside the hands with no slot
		stack.inventoryTick(helper.getLevel(), player, null);

		helper.assertFalse(stack.has(AdventurersCompass.NOTHING_FOUND), "the message should be gone");
		helper.assertFalse(stack.has(AdventurersCompass.NOTHING_FOUND_AT), "its time should be gone");
		helper.succeed();
	}

	@GameTest
	public void reconnectingForgetsTheMessageOfEveryCompassCarried(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack held = showingNothingFound();
		ItemStack carried = showingNothingFound();
		player.getInventory().setItem(player.getInventory().getSelectedSlot(), held);
		player.getInventory().setItem(20, carried);

		ServerCompass.playerJoined(player);

		helper.assertFalse(held.has(AdventurersCompass.NOTHING_FOUND), "the held compass should not show the message again");
		helper.assertFalse(carried.has(AdventurersCompass.NOTHING_FOUND), "the carried compass should not show the message again");
		helper.succeed();
	}

	private static ItemStack showingNothingFound() {
		ItemStack stack = new ItemStack(AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
		CompassStacks.write(stack, CompassState.EMPTY.withNothingFound(NOTHING, Clock.SYSTEM.nanoTime()));
		return stack;
	}

}
