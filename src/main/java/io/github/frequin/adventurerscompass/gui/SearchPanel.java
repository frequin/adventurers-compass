package io.github.frequin.adventurerscompass.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;
import io.github.frequin.adventurerscompass.network.CancelPacket;
import io.github.frequin.adventurerscompass.network.PassPacket;
import io.github.frequin.adventurerscompass.network.SearchPacket;
import io.github.frequin.adventurerscompass.network.TeleportPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The search part of the compass screen's right column: "Search", which turns
 * into "Cancel" while a search runs, "Pass", then the Target panel (name,
 * distance and dimension) with "Teleport". Starting a search closes the
 * screen. It reads the held compass every frame, so it follows the state the
 * server syncs.
 */
@Environment(EnvType.CLIENT)
final class SearchPanel {

	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 5;
	private static final int LINE = 10;
	private static final int TARGET_TEXT_LINES = 3;

	/** Height of the panel, for the screen to place what comes below. */
	static final int HEIGHT = 2 * (BUTTON_HEIGHT + GAP) + TARGET_TEXT_LINES * LINE + GAP + BUTTON_HEIGHT;

	private final Minecraft minecraft;
	private final Player player;
	private final Supplier<ItemStack> compass;
	private int x;
	private int top;
	private Button searchButton;
	private Button passButton;
	private Button teleportButton;
	private boolean searchAllowed;
	private Component searchRefusal;
	private XpThreshold cost = XpThreshold.FREE;

	SearchPanel(Minecraft minecraft, Player player, Supplier<ItemStack> compass) {
		this.minecraft = minecraft;
		this.player = player;
		this.compass = compass;
	}

	/** Adds the widgets at ({@code x}, {@code y}), {@code width} wide. */
	void init(Consumer<AbstractWidget> add, int x, int y, int width) {
		this.x = x;
		this.top = y;
		searchButton = new TransparentButton(x, y, width, BUTTON_HEIGHT, Component.empty(), (onPress) -> {
			if (state().isSearching()) {
				ClientPlayNetworking.send(new CancelPacket());
			} else {
				ClientPlayNetworking.send(new SearchPacket());
				minecraft.gui.setScreen(null);
			}
		});
		passButton = new TransparentButton(x, y + BUTTON_HEIGHT + GAP, width, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.pass"), (onPress) -> {
			ClientPlayNetworking.send(new PassPacket());
			minecraft.gui.setScreen(null);
		});
		teleportButton = new TransparentButton(x, targetTextTop() + TARGET_TEXT_LINES * LINE + GAP, width, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.teleport"), (onPress) -> {
			ClientPlayNetworking.send(new TeleportPacket());
			minecraft.gui.setScreen(null);
		});
		add.accept(searchButton);
		add.accept(passButton);
		add.accept(teleportButton);
		update(searchAllowed, searchRefusal);
	}

	/**
	 * Refreshes the buttons. {@code searchAllowed} is the screen's verdict on the
	 * Selection (something searchable here, enough XP); it gates "Search" and
	 * "Pass". {@code searchRefusal}, when not null, explains a greyed "Search".
	 */
	void update(boolean searchAllowed, Component searchRefusal) {
		this.searchAllowed = searchAllowed;
		this.searchRefusal = searchRefusal;
		if (searchButton == null) {
			return;
		}
		CompassState state = state();
		boolean searching = state.isSearching();
		searchButton.setMessage(searching ? Component.translatable("string.adventurerscompass.cancelSearch") : searchLabel());
		searchButton.active = searching || searchAllowed;
		searchButton.setTooltip(!searching && searchRefusal != null ? Tooltip.create(searchRefusal) : null);
		passButton.active = searchAllowed && state.target() != null;
		teleportButton.visible = AdventurersCompass.canTeleport;
		teleportButton.active = state.target() != null && state.target().dimension().equals(CompassTexts.currentDimension(player));
	}

	/** The XP the Search button announces: the threshold, "≤" when costs vary, nothing when free or in creative. */
	void setCost(XpThreshold cost) {
		this.cost = cost;
	}

	private Component searchLabel() {
		Component search = Component.translatable("string.adventurerscompass.searchSelection");
		if (cost.levels() <= 0) {
			return search;
		}
		return Component.translatable(cost.varies() ? "string.adventurerscompass.searchCostUpTo" : "string.adventurerscompass.searchCost", search, cost.levels());
	}

	/** Draws the Target panel text. */
	void render(GuiGraphicsExtractor guiGraphics, Font font) {
		update(searchAllowed, searchRefusal);
		int y = targetTextTop();
		guiGraphics.text(font, Component.translatable("string.adventurerscompass.target"), x, y, 0xffaaaaaa, true);
		Target target = state().target();
		if (target == null) {
			guiGraphics.text(font, Component.translatable("string.adventurerscompass.noTarget"), x, y + LINE, 0xff808080, true);
			return;
		}
		guiGraphics.text(font, CompassTexts.structureName(target), x, y + LINE, 0xffffffff, true);
		Component where = target.dimension().equals(CompassTexts.currentDimension(player))
				? Component.translatable("string.adventurerscompass.targetHere", CompassTexts.distance(target, player), CompassTexts.dimensionName(target.dimension()))
				: Component.translatable("string.adventurerscompass.targetElsewhere", CompassTexts.dimensionName(target.dimension()));
		guiGraphics.text(font, where, x, y + 2 * LINE, 0xffaaaaaa, true);
	}

	private int targetTextTop() {
		return top + 2 * (BUTTON_HEIGHT + GAP);
	}

	private CompassState state() {
		return CompassStacks.read(compass.get());
	}

}
