package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.IgnoredElement;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.xp.PlayerXp;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The compass screen, variant A of the prototype: the search box, the Selection
 * tree of the Current dimension, "Select all", the count of Selection elements
 * ignored here and the count of Structures of other dimensions on the left; the
 * search actions, "Clear" and the Visited list on the right. Every change to the
 * Selection is sent to the server, which stores it on the compass.
 */
public class AdventurersCompassScreen extends Screen {

	private static final int MARGIN = 10;
	private static final int TREE_TOP = 30;
	private static final int FILTER_HEIGHT = 16;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 5;
	/** Below the tree: the "Select all" row, then the line counting other dimensions. */
	private static final int FOOTER_HEIGHT = 36;

	private final Player player;
	private final SearchPanel searchPanel;
	private Selection selection;
	private Catalogue catalogue;
	private String dimension;
	private int ignoredX;
	private int ignoredY;
	private TransparentEditBox filterBox;
	private SelectionTree tree;
	private Button clearButton;
	private Button selectAllButton;

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
		int treeBottom = height - MARGIN - FOOTER_HEIGHT;
		tree = addRenderableWidget(new SelectionTree(this, minecraft, MARGIN, treeTop, split - MARGIN, treeBottom - treeTop));
		tree.setFilter(TreeFilter.of(filterText), catalogue, dimension);
		filterBox.setResponder(text -> tree.setFilter(TreeFilter.of(text), catalogue, dimension));
		setInitialFocus(filterBox);

		Component selectAll = Component.translatable("string.adventurerscompass.selectAll");
		int selectAllWidth = Math.min(font.width(selectAll) + 16, split - MARGIN);
		selectAllButton = addRenderableWidget(new TransparentButton(MARGIN, treeBottom + 3, selectAllWidth, BUTTON_HEIGHT, selectAll, (onPress) -> {
			updateSelection(selectAll());
		}));
		ignoredX = MARGIN + selectAllWidth + GAP;
		ignoredY = treeBottom + 9;

		// The right column stacks from the top, so "Done" can only be pushed down, never over it
		int clearTop = TREE_TOP + SearchPanel.HEIGHT + GAP;
		int visitedTop = clearTop + BUTTON_HEIGHT + GAP;
		int doneTop = Math.max(height - 30, visitedTop);
		int visitedHeight = doneTop - GAP - visitedTop;
		if (visitedHeight >= VisitedList.CAPTION_HEIGHT + 12) {
			addRenderableWidget(new VisitedList(player, rightX, visitedTop, rightWidth, visitedHeight));
		}
		clearButton = addRenderableWidget(new TransparentButton(rightX, clearTop, rightWidth, BUTTON_HEIGHT, Component.translatable("string.adventurerscompass.clearSelection"), (onPress) -> {
			updateSelection(selection.clear());
		}));

		searchPanel.init(this::addRenderableWidget, rightX, TREE_TOP, rightWidth);
		addRenderableWidget(new TransparentButton(rightX, doneTop, rightWidth, BUTTON_HEIGHT, Component.translatable("gui.done"), (onPress) -> {
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
			String text = Component.translatable("string.adventurerscompass.otherDimensionsHidden", outside).getString();
			guiGraphics.text(font, font.plainSubstrByWidth(text, leftColumnRight() - MARGIN), MARGIN, height - MARGIN - 8, 0xff808080);
		}
		renderIgnored(guiGraphics, mouseX, mouseY);
	}

	/** The count of Selection elements ignored here, beside "Select all"; its tooltip names them. */
	private void renderIgnored(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		List<IgnoredElement> ignored = selection.ignored(catalogue, dimension);
		if (ignored.isEmpty()) {
			return;
		}
		String text = Component.translatable("string.adventurerscompass.ignoredHere", ignored.size()).getString();
		String count = font.plainSubstrByWidth(text, leftColumnRight() - ignoredX);
		guiGraphics.text(font, count, ignoredX, ignoredY, 0xffaaaaaa);
		if (mouseX >= ignoredX && mouseX < ignoredX + font.width(count) && mouseY >= ignoredY - 1 && mouseY < ignoredY + font.lineHeight) {
			guiGraphics.setComponentTooltipForNextFrame(font, ignoredTooltip(ignored), mouseX, mouseY);
		}
	}

	private static List<Component> ignoredTooltip(List<IgnoredElement> ignored) {
		List<Component> lines = new ArrayList<>();
		for (IgnoredElement.Reason reason : IgnoredElement.Reason.values()) {
			List<IgnoredElement> elements = ignored.stream().filter(element -> element.reason() == reason).toList();
			if (!elements.isEmpty()) {
				lines.add(Component.translatable(reason == IgnoredElement.Reason.HIDDEN ? "string.adventurerscompass.ignoredHidden" : "string.adventurerscompass.ignoredAbsent").withColor(0xffaaaaaa));
				elements.forEach(element -> lines.add(Component.literal(StructureUtils.getStructureName(Identifier.parse(element.id())))));
			}
		}
		return lines;
	}

	private int leftColumnRight() {
		return width * 3 / 5;
	}

	/** The Selection with everything the tree lists ticked. */
	private Selection selectAll() {
		return selection.tickAll(tree.listed(catalogue, dimension), catalogue, dimension);
	}

	private void updateButtons() {
		Set<String> searchable = selection.resolve(catalogue, dimension);
		boolean canSearch = selection.canSearchIn(catalogue, dimension);
		// The server sent each Structure's real cost in the Current dimension
		XpThreshold threshold = AdventurersCompass.infiniteXp ? XpThreshold.FREE : XpThreshold.of(searchable.stream().map(id -> AdventurersCompass.xpLevels.getOrDefault(id, 0)).toList());
		boolean affordable = new PlayerXp(player.experienceLevel, AdventurersCompass.infiniteXp).canLaunch(threshold);
		Component refusal = null;
		if (!canSearch && !selection.equals(Selection.EMPTY)) {
			// Only ignored elements: say why nothing can be searched here
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
		selectAllButton.active = !selectAll().equals(selection);
	}

}
