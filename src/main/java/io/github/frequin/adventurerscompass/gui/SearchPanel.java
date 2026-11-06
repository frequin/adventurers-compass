package io.github.frequin.adventurerscompass.gui;

import static io.github.frequin.adventurerscompass.gui.ScreenLayout.BUTTON_HEIGHT;
import static io.github.frequin.adventurerscompass.gui.ScreenLayout.GAP;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;
import io.github.frequin.adventurerscompass.network.CancelPacket;
import io.github.frequin.adventurerscompass.network.ClearTargetPacket;
import io.github.frequin.adventurerscompass.network.MarkVisitedPacket;
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
 * distance and dimension) with "Clear Target", "Mark Visited" and, when allowed,
 * "Teleport" on its own row, so that each label has the whole column. Starting
 * a search closes the screen. It reads the held compass every frame, so it
 * follows the state the server syncs.
 */
@Environment(EnvType.CLIENT)
final class SearchPanel {

	private static final int LINE = 10;
	private static final int TARGET_TEXT_LINES = 3;
	private static final int CLEAR_TARGET_HEIGHT = 12;

	/** Height of the panel, for the screen to place what comes below. */
	static int height(boolean canTeleport) {
		int height = 2 * (BUTTON_HEIGHT + GAP) + TARGET_TEXT_LINES * LINE + GAP + BUTTON_HEIGHT;
		return canTeleport ? height + BUTTON_HEIGHT + GAP : height;
	}

	private final Minecraft minecraft;
	private final Player player;
	private final Supplier<ItemStack> compass;
	private int x;
	private int top;
	private Button searchButton;
	private Button passButton;
	private Button teleportButton;
	private Button markVisitedButton;
	private Button clearTargetButton;
	private int width;
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
		this.width = width;
		searchButton = new TransparentButton(x, y, width, BUTTON_HEIGHT, Component.empty(), (onPress) -> {
			if (state().isSearching()) {
				ClientPlayNetworking.send(new CancelPacket());
			} else {
				search();
			}
		});
		passButton = new TransparentButton(x, y + BUTTON_HEIGHT + GAP, width, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.pass"), (onPress) -> {
			ClientPlayNetworking.send(new PassPacket());
			minecraft.gui.setScreen(null);
		});
		int targetButtonsTop = targetTextTop() + TARGET_TEXT_LINES * LINE + GAP;
		teleportButton = new TransparentButton(x, targetButtonsTop + BUTTON_HEIGHT + GAP, width, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.teleport"), (onPress) -> {
			ClientPlayNetworking.send(new TeleportPacket());
			minecraft.gui.setScreen(null);
		});
		markVisitedButton = new TransparentButton(x, targetButtonsTop, width, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.markVisited"), (onPress) -> {
			ClientPlayNetworking.send(new MarkVisitedPacket());
		});
		// On the "Target" caption line, right-aligned, so it takes no room from the rest of the column
		Component clearTarget = Component.translatable("string.adventurerscompass.clearTarget");
		int captionWidth = minecraft.font.width(Component.translatable("string.adventurerscompass.target"));
		int clearTargetWidth = ScreenLayout.buttonWidth(minecraft.font.width(clearTarget), ScreenLayout.clearTargetRoom(width, captionWidth));
		clearTargetButton = new TransparentButton(x + width - clearTargetWidth, targetTextTop() - 2, clearTargetWidth, CLEAR_TARGET_HEIGHT, clearTarget, (onPress) -> {
			ClientPlayNetworking.send(new ClearTargetPacket());
		});
		add.accept(searchButton);
		add.accept(passButton);
		add.accept(teleportButton);
		add.accept(markVisitedButton);
		add.accept(clearTargetButton);
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
		if (state.isBroken(AdventurersCompassItem.durability(compass.get()))) {
			// A broken compass refuses "Search" and "Pass" whatever the Selection; it keeps its Target
			searchAllowed = false;
			searchRefusal = Component.translatable("string.adventurerscompass.broken");
		}
		boolean searching = state.isSearching();
		searchButton.setMessage(searching ? Component.translatable("string.adventurerscompass.cancelSearch") : searchLabel());
		searchButton.active = searching || searchAllowed;
		searchButton.setTooltip(!searching && searchRefusal != null ? Tooltip.create(searchRefusal) : null);
		passButton.active = searchAllowed && state.target() != null;
		teleportButton.visible = AdventurersCompass.canTeleport;
		teleportButton.active = state.target() != null && state.target().dimension().equals(CompassTexts.currentDimension(player));
		markVisitedButton.active = state.target() != null && !targetVisited(state);
		clearTargetButton.visible = state.target() != null;
	}

	/**
	 * The keyboard shortcut for "Search": starts a search when the button would,
	 * never cancels one. Returns whether it searched.
	 */
	boolean searchFromKeyboard() {
		update(searchAllowed, searchRefusal);
		if (searchButton == null || !searchButton.active || state().isSearching()) {
			return false;
		}
		search();
		return true;
	}

	private void search() {
		ClientPlayNetworking.send(new SearchPacket());
		minecraft.gui.setScreen(null);
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
		Component name = targetVisited(state())
				? Component.translatable("string.adventurerscompass.targetVisited", CompassTexts.structureName(target))
				: CompassTexts.structureName(target);
		guiGraphics.text(font, name, x, y + LINE, 0xffffffff, true);
		Component where = target.dimension().equals(CompassTexts.currentDimension(player))
				? Component.translatable("string.adventurerscompass.targetHere", CompassTexts.distance(target, player), CompassTexts.dimensionName(target.dimension()))
				: Component.translatable("string.adventurerscompass.targetElsewhere", CompassTexts.dimensionName(target.dimension()));
		guiGraphics.text(font, where, x, y + 2 * LINE, 0xffaaaaaa, true);
	}

	private int targetTextTop() {
		return top + 2 * (BUTTON_HEIGHT + GAP);
	}

	private boolean targetVisited(CompassState state) {
		return state.target() != null && CompassTexts.visited(player).contains(state.target().instance());
	}

	private CompassState state() {
		return CompassStacks.read(compass.get());
	}

}
