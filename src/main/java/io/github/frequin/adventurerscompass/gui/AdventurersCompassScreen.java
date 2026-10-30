package io.github.frequin.adventurerscompass.gui;

import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The compass screen, variant A of the prototype: the Selection tree of the
 * Current dimension on the left, the search actions on the right. Every change
 * to the Selection is sent to the server, which stores it on the compass.
 */
public class AdventurersCompassScreen extends Screen {

	private static final int MARGIN = 10;
	private static final int TREE_TOP = 30;

	private final Player player;
	private final SearchPanel searchPanel;
	private Selection selection;
	private Catalogue catalogue;
	private String dimension;
	private SelectionTree tree;

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

		tree = addRenderableWidget(new SelectionTree(this, minecraft, MARGIN, TREE_TOP, split - MARGIN, height - TREE_TOP - MARGIN));
		tree.refresh(catalogue, dimension);

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
			updateButtons();
			AdventurersCompass.synced = false;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.centeredText(font, title, (MARGIN + leftColumnRight()) / 2, 12, 0xffffffff);
		searchPanel.render(guiGraphics, font);
	}

	private int leftColumnRight() {
		return width * 3 / 5;
	}

	private void updateButtons() {
		Set<String> searchable = selection.resolve(catalogue, dimension);
		boolean affordable = AdventurersCompass.infiniteXp || player.experienceLevel >= maxXpLevels(searchable);
		searchPanel.update(!searchable.isEmpty() && affordable);
	}

	private static int maxXpLevels(Set<String> structureIds) {
		int max = 0;
		for (String structureId : structureIds) {
			max = Math.max(max, AdventurersCompass.xpLevels.getOrDefault(structureId, 0));
		}
		return max;
	}

}
