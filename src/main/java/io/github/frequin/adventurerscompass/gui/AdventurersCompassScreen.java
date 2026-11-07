package io.github.frequin.adventurerscompass.gui;

import static io.github.frequin.adventurerscompass.gui.ScreenLayout.BUTTON_HEIGHT;
import static io.github.frequin.adventurerscompass.gui.ScreenLayout.GAP;
import static io.github.frequin.adventurerscompass.gui.ScreenLayout.MARGIN;
import static io.github.frequin.adventurerscompass.gui.ScreenLayout.SMALL_BUTTON_PADDING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.rows.InstanceRow;
import io.github.frequin.adventurerscompass.domain.rows.InstanceRows;
import io.github.frequin.adventurerscompass.domain.rows.InstanceSort;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.selection.UnsearchableElement;
import io.github.frequin.adventurerscompass.domain.xp.PlayerXp;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.ClearSkippedPacket;
import io.github.frequin.adventurerscompass.network.ClearVisitedPacket;
import io.github.frequin.adventurerscompass.network.RemoveSkippedPacket;
import io.github.frequin.adventurerscompass.network.UnmarkVisitedPacket;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The compass screen: three tabs across the top. The Search tab holds the
 * search box, the Selection tree of the Current dimension, "Select all" and
 * "Clear", the count of Selection elements unsearchable here and the count of
 * Structures of other dimensions on the left; the search actions on the right.
 * The Visited and Skipped tabs hold a full-width list with a filter, a sort
 * button and a button that clears the whole list, over a footer with the Target
 * and "Done". Every change to the Selection is sent to the server, which stores
 * it on the compass.
 */
public class AdventurersCompassScreen extends Screen {

	private static final int TABS_TOP = 8;
	private static final int TABS_HEIGHT = 18;
	private static final int TREE_TOP = 30;
	private static final int FILTER_HEIGHT = 16;
	/** Below the tree: the "Select all" and "Clear" row, then the unsearchable and other dimensions lines. */
	private static final int FOOTER_HEIGHT = 46;
	private static final int FOOTER_LINE = 10;
	/** Below the list of the Visited and Skipped tabs: the Target and "Done". */
	private static final int LIST_FOOTER_HEIGHT = 16;
	private static final Component DONE = Component.translatable("gui.done");

	private enum Tab {
		SEARCH, VISITED, SKIPPED
	}

	/** The rows of a list tab, filtered and sorted. */
	private interface RowSource {
		List<InstanceRow> rows(InstanceRows rows, TreeFilter filter, InstanceSort sort);
	}

	/** What the Visited and Skipped tabs do differently; the rest of a list tab is shared. Each keeps its sort. */
	private static final class ListTab {

		private final List<InstanceSort> orders;
		private final String crossTooltip;
		private final String clearAllLabel;
		/** Also the start of the keys of its warning and its action. */
		private final String confirmClearAll;
		private final String emptyMessage;
		private final IntSupplier size;
		private final RowSource source;
		private final Consumer<InstanceKey> removeOne;
		private final Runnable removeAll;
		private InstanceSort sort;

		ListTab(List<InstanceSort> orders, String crossTooltip, String clearAllLabel, String confirmClearAll, String emptyMessage, IntSupplier size, RowSource source, Consumer<InstanceKey> removeOne, Runnable removeAll) {
			this.orders = orders;
			this.crossTooltip = crossTooltip;
			this.clearAllLabel = clearAllLabel;
			this.confirmClearAll = confirmClearAll;
			this.emptyMessage = emptyMessage;
			this.size = size;
			this.source = source;
			this.removeOne = removeOne;
			this.removeAll = removeAll;
			this.sort = orders.getFirst();
		}

	}

	private final Player player;
	private final SearchPanel searchPanel;
	/** Kept here so that the tree stays open as it was when the player comes back to its tab. */
	private final Set<String> expandedGroups = new HashSet<String>();
	private final List<Button> tabButtons = new ArrayList<Button>();
	private Tab tab = Tab.SEARCH;
	private final ListTab visitedTab;
	private final ListTab skippedTab;
	private Selection selection;
	private Catalogue catalogue;
	private String dimension;
	private TransparentEditBox filterBox;
	private SelectionTree tree;
	private Button clearButton;
	private Button selectAllButton;
	private InstanceList instanceList;
	private Button sortButton;
	private Button clearAllButton;
	private int footerTextWidth;
	private boolean laidOutForTeleport;

	public AdventurersCompassScreen(Player player, ItemStack stack, AdventurersCompassItem adventurersCompass) {
		super(Component.translatable("string.adventurerscompass.composeSelection"));
		this.player = player;
		this.selection = stack.getOrDefault(AdventurersCompass.SELECTION, Selection.EMPTY);
		this.searchPanel = new SearchPanel(Minecraft.getInstance(), player, this::compass);
		// Last known catalogue until the SyncPacket sent on opening arrives
		this.catalogue = AdventurersCompass.catalogue;
		this.dimension = AdventurersCompass.currentDimension;
		this.visitedTab = new ListTab(InstanceSort.VISITED_ORDERS,
				"string.adventurerscompass.unmarkVisited", "string.adventurerscompass.unmarkAll", "string.adventurerscompass.confirmUnmarkAll", "string.adventurerscompass.noVisited",
				this::visitedCount,
				(rows, filter, sort) -> rows.visited(CompassTexts.visited(player), filter, sort),
				instance -> ClientPlayNetworking.send(new UnmarkVisitedPacket(instance)),
				() -> ClientPlayNetworking.send(new ClearVisitedPacket()));
		this.skippedTab = new ListTab(InstanceSort.SKIPPED_ORDERS,
				"string.adventurerscompass.removeSkipped", "string.adventurerscompass.removeAll", "string.adventurerscompass.confirmRemoveAll", "string.adventurerscompass.noSkipped",
				() -> skipped().size(),
				(rows, filter, sort) -> rows.skipped(skipped(), filter, sort),
				instance -> ClientPlayNetworking.send(new RemoveSkippedPacket(instance)),
				() -> ClientPlayNetworking.send(new ClearSkippedPacket()));
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
		addTabs();
		String filterText = filterBox == null ? "" : filterBox.getValue();
		if (tab == Tab.SEARCH) {
			initSearchTab(filterText);
		} else {
			initListTab(filterText);
		}
		updateButtons();
	}

	private void addTabs() {
		tabButtons.clear();
		int tabWidth = ScreenLayout.tabWidth(width);
		for (Tab each : Tab.values()) {
			int x = MARGIN + each.ordinal() * (tabWidth + GAP);
			tabButtons.add(addRenderableWidget(new TabButton(x, TABS_TOP, tabWidth, TABS_HEIGHT, tabLabel(each), each == tab, (onPress) -> {
				showTab(each);
			})));
		}
	}

	private Component tabLabel(Tab each) {
		return switch (each) {
			case SEARCH -> Component.translatable("string.adventurerscompass.tabSearch");
			case VISITED -> Component.translatable("string.adventurerscompass.visitedList", visitedCount());
			case SKIPPED -> Component.translatable("string.adventurerscompass.skippedList", skipped().size());
		};
	}

	/** Opens a tab with an empty filter; each tab keeps its sort and the tree stays open as it was. */
	private void showTab(Tab next) {
		if (next != tab) {
			tab = next;
			filterBox = null;
			rebuildWidgets();
		}
	}

	private void initSearchTab(String filterText) {
		laidOutForTeleport = AdventurersCompass.canTeleport;
		int split = leftColumnRight();
		int rightX = split + MARGIN;
		int rightWidth = ScreenLayout.rightColumnWidth(width);

		filterBox = addRenderableWidget(new TransparentEditBox(font, MARGIN, TREE_TOP, split - MARGIN, FILTER_HEIGHT, Component.translatable("string.adventurerscompass.filterHint").withColor(0xff808080)));
		filterBox.setValue(filterText);
		int treeTop = TREE_TOP + FILTER_HEIGHT + 4;
		int treeBottom = height - MARGIN - FOOTER_HEIGHT;
		tree = addRenderableWidget(new SelectionTree(this, minecraft, MARGIN, treeTop, split - MARGIN, treeBottom - treeTop, expandedGroups));
		tree.setFilter(TreeFilter.of(filterText), catalogue, dimension);
		filterBox.setResponder(text -> tree.setFilter(TreeFilter.of(text), catalogue, dimension));
		setInitialFocus(filterBox);

		Component selectAll = Component.translatable("string.adventurerscompass.selectAll");
		int selectAllWidth = font.width(selectAll) + SMALL_BUTTON_PADDING;
		selectAllButton = addRenderableWidget(new TransparentButton(MARGIN, treeBottom + 3, selectAllWidth, BUTTON_HEIGHT, selectAll, (onPress) -> {
			updateSelection(selectAll());
		}));
		Component clear = Component.translatable("string.adventurerscompass.clearSelection");
		clearButton = addRenderableWidget(new TransparentButton(MARGIN + selectAllWidth + GAP, treeBottom + 3, font.width(clear) + SMALL_BUTTON_PADDING, BUTTON_HEIGHT, clear, (onPress) -> {
			updateSelection(selection.clear());
		}));

		// The right column stacks from the top, so "Done" can only be pushed down, never over it
		int doneTop = Math.max(height - 30, TREE_TOP + SearchPanel.height(AdventurersCompass.canTeleport) + GAP);

		searchPanel.init(this::addRenderableWidget, rightX, TREE_TOP, rightWidth);
		addDoneButton(rightX, doneTop, rightWidth, BUTTON_HEIGHT);
	}

	private void initListTab(String filterText) {
		filterBox = addRenderableWidget(new TransparentEditBox(font, MARGIN, TREE_TOP, width - 2 * MARGIN, FILTER_HEIGHT, Component.translatable("string.adventurerscompass.filterRowsHint").withColor(0xff808080)));
		filterBox.setValue(filterText);
		filterBox.setResponder(text -> {
			refreshRows();
			instanceList.setScrollAmount(0);
		});
		setInitialFocus(filterBox);

		ListTab list = listTab();
		int sortWidth = list.orders.stream().mapToInt(order -> font.width(sortLabel(order))).max().orElse(0) + SMALL_BUTTON_PADDING;
		sortButton = addRenderableWidget(new TransparentButton(0, TREE_TOP, sortWidth, FILTER_HEIGHT, sortLabel(list.sort), (onPress) -> {
			cycleSort();
		}));
		clearAllButton = addRenderableWidget(new TransparentButton(0, TREE_TOP, 0, FILTER_HEIGHT, Component.empty(), (onPress) -> {
			confirmClearAll();
		}));

		int listTop = TREE_TOP + FILTER_HEIGHT + 4;
		int footerTop = height - MARGIN - LIST_FOOTER_HEIGHT;
		instanceList = addRenderableWidget(new InstanceList(minecraft, MARGIN, listTop, width - 2 * MARGIN, footerTop - 4 - listTop, Component.translatable(list.crossTooltip), list.removeOne));

		int doneWidth = font.width(DONE) + 2 * SMALL_BUTTON_PADDING;
		int doneX = width - MARGIN - doneWidth;
		footerTextWidth = doneX - GAP - MARGIN;
		addDoneButton(doneX, footerTop, doneWidth, LIST_FOOTER_HEIGHT);
	}

	private void addDoneButton(int x, int y, int buttonWidth, int buttonHeight) {
		addRenderableWidget(new TransparentButton(x, y, buttonWidth, buttonHeight, DONE, (onPress) -> {
			minecraft.gui.setScreen(null);
		}));
	}

	/** The open list tab; only call on one. */
	private ListTab listTab() {
		return tab == Tab.VISITED ? visitedTab : skippedTab;
	}

	private void cycleSort() {
		ListTab list = listTab();
		list.sort = list.sort.next(list.orders);
		sortButton.setMessage(sortLabel(list.sort));
		refreshRows();
		instanceList.setScrollAmount(0);
	}

	private static Component sortLabel(InstanceSort order) {
		return Component.translatable(switch (order) {
			case RECENT -> "string.adventurerscompass.sortRecent";
			case NEAREST -> "string.adventurerscompass.sortNearest";
			case NAME -> "string.adventurerscompass.sortName";
		});
	}

	/** Asks before clearing the whole list, filter or not. Both answers come back to this screen, on this tab. */
	private void confirmClearAll() {
		ListTab list = listTab();
		Component question = Component.translatable(list.confirmClearAll, list.size.getAsInt());
		Component warning = Component.translatable(list.confirmClearAll + "Warning");
		Component action = Component.translatable(list.confirmClearAll + "Action").withStyle(ChatFormatting.RED);
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				list.removeAll.run();
			}
			minecraft.gui.setScreen(this);
		}, question, warning, action, CommonComponents.GUI_CANCEL));
	}

	@Override
	public void tick() {
		if (AdventurersCompass.synced) {
			catalogue = AdventurersCompass.catalogue;
			dimension = AdventurersCompass.currentDimension;
			AdventurersCompass.synced = false;
			if (tab == Tab.SEARCH && laidOutForTeleport != AdventurersCompass.canTeleport) {
				// "Teleport" takes or gives back a row of the right column
				rebuildWidgets();
			} else if (tab == Tab.SEARCH) {
				tree.refresh(catalogue, dimension);
			}
		}
		// Levels, Visited and Skipped can change while the screen is open: re-read them every tick
		updateButtons();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Enter in the empty search box searches the Selection
		if (tab == Tab.SEARCH && event.isConfirmation() && filterBox.isFocused() && filterBox.getValue().isEmpty() && searchPanel.searchFromKeyboard()) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		if (tab != Tab.SEARCH) {
			renderTargetFooter(guiGraphics);
			return;
		}
		searchPanel.render(guiGraphics, font);
		int outside = catalogue.countOutside(dimension);
		if (outside > 0) {
			String text = Component.translatable("string.adventurerscompass.otherDimensionsHidden", outside).getString();
			guiGraphics.text(font, font.plainSubstrByWidth(text, leftColumnRight() - MARGIN), MARGIN, height - MARGIN - 8, 0xff808080);
		}
		renderUnsearchable(guiGraphics, mouseX, mouseY);
	}

	/** The Target as text, left of "Done": "◎ Structure · distance", or its dimension when elsewhere. */
	private void renderTargetFooter(GuiGraphicsExtractor guiGraphics) {
		Target target = CompassStacks.read(compass()).target();
		Component text = target == null ? Component.translatable("string.adventurerscompass.noTarget") : Component.translatable("string.adventurerscompass.targetFooter", CompassTexts.structureName(target), targetWhere(target));
		guiGraphics.text(font, font.plainSubstrByWidth(text.getString(), footerTextWidth), MARGIN, height - MARGIN - (LIST_FOOTER_HEIGHT + font.lineHeight) / 2 + 1, 0xffaaaaaa);
	}

	private Component targetWhere(Target target) {
		if (target.dimension().equals(CompassTexts.currentDimension(player))) {
			return Component.translatable("string.adventurerscompass.distance", CompassTexts.distance(target, player));
		}
		return CompassTexts.dimensionName(target.dimension());
	}

	/** The count of Selection elements unsearchable here, under "Select all"; its tooltip names them. */
	private void renderUnsearchable(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		List<UnsearchableElement> unsearchable = selection.unsearchable(catalogue, dimension);
		if (unsearchable.isEmpty()) {
			return;
		}
		String text = Component.translatable("string.adventurerscompass.unsearchableHere", unsearchable.size()).getString();
		String count = font.plainSubstrByWidth(text, ScreenLayout.leftColumnWidth(width));
		int y = height - MARGIN - 8 - FOOTER_LINE;
		guiGraphics.text(font, count, MARGIN, y, 0xffaaaaaa);
		if (mouseX >= MARGIN && mouseX < MARGIN + font.width(count) && mouseY >= y - 1 && mouseY < y + font.lineHeight) {
			guiGraphics.setComponentTooltipForNextFrame(font, unsearchableTooltip(unsearchable), mouseX, mouseY);
		}
	}

	private List<Component> unsearchableTooltip(List<UnsearchableElement> unsearchable) {
		List<Component> lines = new ArrayList<>();
		for (UnsearchableElement.Reason reason : UnsearchableElement.Reason.values()) {
			List<UnsearchableElement> elements = unsearchable.stream().filter(element -> element.reason() == reason).toList();
			if (!elements.isEmpty()) {
				lines.add(Component.translatable(reason == UnsearchableElement.Reason.HIDDEN ? "string.adventurerscompass.unsearchableHidden" : "string.adventurerscompass.unsearchableAbsent").withColor(0xffaaaaaa));
				elements.forEach(element -> lines.add(Component.literal(unsearchableName(element))));
			}
		}
		return lines;
	}

	private static String unsearchableName(UnsearchableElement element) {
		return switch (element.kind()) {
			case GROUP -> StructureUtils.getGroupName(Identifier.parse(element.id()));
			case STRUCTURE -> structureName(element.id());
		};
	}

	private static String structureName(String id) {
		return StructureUtils.getStructureName(Identifier.parse(id));
	}

	private int leftColumnRight() {
		return ScreenLayout.leftColumnRight(width);
	}

	/** The Selection with everything the tree lists ticked. */
	private Selection selectAll() {
		return selection.tickAll(tree.listed(catalogue, dimension), catalogue, dimension);
	}

	private ItemStack compass() {
		return ItemUtils.getHeldItem(player, AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
	}

	private Set<InstanceKey> skipped() {
		return CompassStacks.read(compass()).skipped();
	}

	private int visitedCount() {
		return CompassTexts.visited(player).asSet().size();
	}

	private void updateButtons() {
		for (Tab each : Tab.values()) {
			tabButtons.get(each.ordinal()).setMessage(tabLabel(each));
		}
		if (tab == Tab.SEARCH) {
			updateSearchButtons();
		} else {
			updateListBar();
			refreshRows();
		}
	}

	/** The bar of the list tabs: the filter takes what the sort button and "clear all", whose count changes, leave. */
	private void updateListBar() {
		ListTab list = listTab();
		int count = list.size.getAsInt();
		Component label = Component.translatable(list.clearAllLabel, count);
		clearAllButton.setMessage(label);
		clearAllButton.active = count > 0;
		clearAllButton.setWidth(font.width(label) + SMALL_BUTTON_PADDING);
		clearAllButton.setX(width - MARGIN - clearAllButton.getWidth());
		sortButton.setX(clearAllButton.getX() - GAP - sortButton.getWidth());
		filterBox.setWidth(sortButton.getX() - GAP - MARGIN);
	}

	private void refreshRows() {
		BlockPos pos = player.blockPosition();
		InstanceRows rows = new InstanceRows(CompassTexts.currentDimension(player), new BlockXZ(pos.getX(), pos.getZ()), catalogue, AdventurersCompassScreen::structureName);
		ListTab list = listTab();
		List<InstanceRow> shown = list.source.rows(rows, TreeFilter.of(filterBox.getValue()), list.sort);
		// The count ignores the filter: an empty list and a filter that keeps nothing read differently
		instanceList.setRows(shown, Component.translatable(list.size.getAsInt() > 0 ? "string.adventurerscompass.noRows" : list.emptyMessage));
	}

	private void updateSearchButtons() {
		Set<String> searchable = selection.resolve(catalogue, dimension);
		boolean canSearch = selection.canSearchIn(catalogue, dimension);
		// The server sent each Structure's real cost in the Current dimension
		XpThreshold threshold = AdventurersCompass.infiniteXp ? XpThreshold.FREE : XpThreshold.of(searchable.stream().map(id -> AdventurersCompass.xpLevels.getOrDefault(id, 0)).toList());
		boolean affordable = new PlayerXp(player.experienceLevel, AdventurersCompass.infiniteXp).canLaunch(threshold);
		Component refusal = null;
		if (!canSearch && !selection.equals(Selection.EMPTY)) {
			// Only unsearchable elements: say why nothing can be searched here
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
