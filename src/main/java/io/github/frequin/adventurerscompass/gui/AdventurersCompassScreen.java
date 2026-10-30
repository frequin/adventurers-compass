package io.github.frequin.adventurerscompass.gui;

import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.SearchForNextPacket;
import io.github.frequin.adventurerscompass.network.SearchPacket;
import io.github.frequin.adventurerscompass.network.TeleportPacket;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.util.CompassState;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
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
	/** Top of the Selection panel (caption, chips, Clear) in the right column, below the search actions. */
	private static final int SELECTION_PANEL_TOP = TREE_TOP + 80;

	private final Player player;
	private final boolean hasTarget;
	private Selection selection;
	private Catalogue catalogue;
	private String dimension;
	private TransparentEditBox filterBox;
	private SelectionTree tree;
	private Button clearButton;
	private Button searchButton;
	private Button searchForNextButton;
	private Button teleportButton;

	public AdventurersCompassScreen(Player player, ItemStack stack, AdventurersCompassItem adventurersCompass) {
		super(Component.translatable("string.adventurerscompass.composeSelection"));
		this.player = player;
		this.selection = stack.getOrDefault(AdventurersCompass.SELECTION, Selection.EMPTY);
		this.hasTarget = adventurersCompass.getCompassState(stack) == CompassState.FOUND;
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
		int clearTop = height - 30 - 25;
		addRenderableWidget(new SelectionChips(this, rightX, chipsTop, rightWidth, clearTop - chipsTop - 5));
		clearButton = addRenderableWidget(new TransparentButton(rightX, clearTop, rightWidth, 20, Component.translatable("string.adventurerscompass.clearSelection"), (onPress) -> {
			updateSelection(selection.clear());
		}));

		searchButton = addRenderableWidget(new TransparentButton(rightX, TREE_TOP, rightWidth, 20, Component.translatable("string.adventurerscompass.searchSelection"), (onPress) -> {
			ClientPlayNetworking.send(new SearchPacket());
			minecraft.gui.setScreen(null);
		}));
		searchForNextButton = addRenderableWidget(new TransparentButton(rightX, TREE_TOP + 25, rightWidth, 20, Component.translatable("string.adventurerscompass.searchForNext"), (onPress) -> {
			ClientPlayNetworking.send(new SearchForNextPacket());
			minecraft.gui.setScreen(null);
		}));
		teleportButton = addRenderableWidget(new TransparentButton(rightX, TREE_TOP + 50, rightWidth, 20, Component.translatable("string.adventurerscompass.teleport"), (onPress) -> {
			ClientPlayNetworking.send(new TeleportPacket());
			minecraft.gui.setScreen(null);
		}));
		addRenderableWidget(new TransparentButton(rightX, height - 30, rightWidth, 20, Component.translatable("gui.cancel"), (onPress) -> {
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
			updateButtons();
			AdventurersCompass.synced = false;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.centeredText(font, title, (MARGIN + leftColumnRight()) / 2, 12, 0xffffffff);
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
		boolean affordable = AdventurersCompass.infiniteXp || player.experienceLevel >= maxXpLevels(searchable);
		searchButton.active = canSearch && affordable;
		// Greyed chips only: say why nothing can be searched here
		searchButton.setTooltip(!canSearch && !selection.equals(Selection.EMPTY) ? Tooltip.create(Component.translatable("string.adventurerscompass.nothingSearchableHere")) : null);
		searchForNextButton.visible = AdventurersCompass.maxNextSearches > 0;
		searchForNextButton.active = hasTarget && canSearch && affordable;
		clearButton.active = !selection.equals(Selection.EMPTY);
		teleportButton.visible = AdventurersCompass.canTeleport;
		teleportButton.active = hasTarget;
	}

	private static int maxXpLevels(Set<String> structureIds) {
		int max = 0;
		for (String structureId : structureIds) {
			max = Math.max(max, AdventurersCompass.xpLevels.getOrDefault(structureId, 0));
		}
		return max;
	}

}
