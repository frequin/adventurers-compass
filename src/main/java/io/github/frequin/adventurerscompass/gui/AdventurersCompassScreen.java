package io.github.frequin.adventurerscompass.gui;

import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.xp.PlayerXp;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The compass screen, variant A of the prototype: the search box, the Selection
 * tree of the Current dimension and the count of Structures of other dimensions
 * on the left; the search actions, then the Selection chips and "Clear" on the
 * right. Every change to the Selection is sent to the server, which stores it
 * on the compass.
 */
public class AdventurersCompassScreen extends Screen {

	private static final int MARGIN = 10;
	private static final int TREE_TOP = 30;
	private static final int FILTER_HEIGHT = 16;
	private static final int FOOTER_HEIGHT = 12;
	/** Top of the Selection panel (caption, chips, Clear) in the right column, below the search actions and the Target. */
	private static final int SELECTION_PANEL_TOP = TREE_TOP + SearchPanel.HEIGHT + 8;

	private final Player player;
	private final SearchPanel searchPanel;
	private Selection selection;
	private Catalogue catalogue;
	private String dimension;
	private TransparentEditBox filterBox;
	private SelectionTree tree;
	private Button clearButton;

	public AdventurersCompassScreen(Player player, ItemStack stack, AdventurersCompassItem adventurersCompass) {
		super(Component.translatable("string.adventurerscompass.composeSelection"));
		this.player = player;
		this.selection = stack.getOrDefault(AdventurersCompass.SELECTION, Selection.EMPTY);
		this.searchPanel = new SearchPanel(Minecraft.getInstance(), player, () -> ItemUtils.getHeldItem(player, AdventurersCompass.ADVENTURERS_COMPASS_ITEM));
		// Last known catalogue until the SyncPacket sent on opening arrives
		this.catalogue = AdventurersCompass.catalogue;
		this.dimension = AdventurersCompass.currentDimension;
	}

	public Selection selection() {
		return selection;
	}

	public Catalogue catalogue() {
		return catalogue;
	}

	public String dimension() {
		return dimension;
	}

	/** Applies a Selection edited in the tree, here and on the compass. */
	public void updateSelection(Selection next) {
		selection = next;
		ClientPlayNetworking.send(new UpdateSelectionPacket(next));
		// Rows read the Selection when drawn, so the tree needs no rebuild
		updateButtons();
	}

	@Override
	protected void init() {
		clearWidgets();
		int split = leftColumnRight();
		int rightX = split + MARGIN;
		int rightWidth = width - rightX - MARGIN;

		String filterText = filterBox == null ? "" : filterBox.getValue();
		filterBox = addRenderableWidget(new TransparentEditBox(font, MARGIN, TREE_TOP, split - MARGIN, FILTER_HEIGHT, Component.translatable("string.adventurerscompass.filterHint").withColor(0xff808080)));
		filterBox.setValue(filterText);
		int treeTop = TREE_TOP + FILTER_HEIGHT + 4;
		tree = addRenderableWidget(new SelectionTree(this, minecraft, MARGIN, treeTop, split - MARGIN, height - treeTop - MARGIN - FOOTER_HEIGHT));
		tree.setFilter(TreeFilter.of(filterText), catalogue, dimension);
		filterBox.setResponder(text -> tree.setFilter(TreeFilter.of(text), catalogue, dimension));
		setInitialFocus(filterBox);

		int chipsTop = SELECTION_PANEL_TOP + 12;
		// The space down to "Done" is shared between the chips (with "Clear") and the Visited list
		int shared = height - 30 - 5 - chipsTop - 25 - VisitedList.CAPTION_HEIGHT;
		int chipsHeight = Math.max(12, shared / 2);
		int clearTop = chipsTop + chipsHeight + 5;
		addRenderableWidget(new SelectionChips(this, rightX, chipsTop, rightWidth, chipsHeight));
		int visitedTop = clearTop + 25;
		addRenderableWidget(new VisitedList(player, rightX, visitedTop, rightWidth, Math.max(VisitedList.CAPTION_HEIGHT + 12, height - 35 - visitedTop)));
		clearButton = addRenderableWidget(new TransparentButton(rightX, clearTop, rightWidth, 20, Component.translatable("string.adventurerscompass.clearSelection"), (onPress) -> {
			updateSelection(selection.clear());
		}));

		searchPanel.init(this::addRenderableWidget, rightX, TREE_TOP, rightWidth);
		addRenderableWidget(new TransparentButton(rightX, height - 30, rightWidth, 20, Component.translatable("gui.done"), (onPress) -> {
			minecraft.gui.setScreen(null);
		}));
		updateButtons();
	}

	@Override
	public void tick() {
		if (AdventurersCompass.synced) {
			catalogue = AdventurersCompass.catalogue;
			dimension = AdventurersCompass.currentDimension;
			tree.refresh(catalogue, dimension);
			AdventurersCompass.synced = false;
		}
		// Levels can change while the screen is open: re-read them every tick
		updateButtons();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Prototype variant A: Enter in the empty search box searches the Selection
		if (event.isConfirmation() && filterBox.isFocused() && filterBox.getValue().isEmpty() && searchPanel.searchFromKeyboard()) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.centeredText(font, title, (MARGIN + leftColumnRight()) / 2, 12, 0xffffffff);
		searchPanel.render(guiGraphics, font);
		int outside = catalogue.countOutside(dimension);
		if (outside > 0) {
			guiGraphics.text(font, Component.translatable("string.adventurerscompass.otherDimensionsHidden", outside), MARGIN, height - MARGIN - 8, 0xff808080);
		}
		guiGraphics.text(font, Component.translatable("string.adventurerscompass.selection"), leftColumnRight() + MARGIN, SELECTION_PANEL_TOP, 0xffffffff);
	}

	private int leftColumnRight() {
		return width * 3 / 5;
	}

	private void updateButtons() {
		Set<String> searchable = selection.resolve(catalogue, dimension);
		boolean canSearch = selection.canSearchIn(catalogue, dimension);
		// The server sent each Structure's real cost in the Current dimension
		XpThreshold threshold = AdventurersCompass.infiniteXp ? XpThreshold.FREE : XpThreshold.of(searchable.stream().map(id -> AdventurersCompass.xpLevels.getOrDefault(id, 0)).toList());
		boolean affordable = new PlayerXp(player.experienceLevel, AdventurersCompass.infiniteXp).canLaunch(threshold);
		Component refusal = null;
		if (!canSearch && !selection.equals(Selection.EMPTY)) {
			// Greyed chips only: say why nothing can be searched here
			refusal = Component.translatable("string.adventurerscompass.nothingSearchableHere");
		} else if (canSearch && !affordable) {
			refusal = Component.translatable("string.adventurerscompass.notEnoughXp", threshold.levels());
		}
		searchPanel.setCost(threshold);
		if (AdventurersCompass.dimensionDisabled) {
			// disabledDimensions: Search and Pass greyed whatever the Selection
			canSearch = false;
			refusal = Component.translatable("string.adventurerscompass.disabledDimension");
		}
		searchPanel.update(canSearch && affordable, refusal);
		clearButton.active = !selection.equals(Selection.EMPTY);
	}

}
