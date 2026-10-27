package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.SearchForNextPacket;
import io.github.frequin.adventurerscompass.network.SearchPacket;
import io.github.frequin.adventurerscompass.network.TeleportPacket;
import io.github.frequin.adventurerscompass.sorting.ISorting;
import io.github.frequin.adventurerscompass.sorting.NameSorting;
import io.github.frequin.adventurerscompass.util.CompassState;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AdventurersCompassScreen extends Screen {

	private Level level;
	private Player player;
	private List<Identifier> allowedStructures;
	private List<Identifier> structuresMatchingSearch;
	private Identifier foundStructureId;
	private Button searchButton;
	private Button searchForNextButton;
	private Button searchForGroupButton;
	private Button sortByButton;
	private Button teleportButton;
	private Button cancelButton;
	private TransparentEditBox searchBox;
	private StructureSearchList selectionList;
	private ISorting sortingCategory;

	public AdventurersCompassScreen(Level level, Player player, ItemStack stack, AdventurersCompassItem adventurersCompass, List<Identifier> allowedStructures) {
		super(Component.translatable("string.adventurerscompass.selectStructure"));
		this.level = level;
		this.player = player;

		this.allowedStructures = new ArrayList<Identifier>(allowedStructures);
		structuresMatchingSearch = new ArrayList<Identifier>(this.allowedStructures);
		sortingCategory = new NameSorting();

		if (adventurersCompass.getCompassState(stack) == CompassState.FOUND) {
			String foundStructureIdStr = stack.getOrDefault(AdventurersCompass.STRUCTURE_ID, null);
			if (foundStructureIdStr != null) {
				foundStructureId = Identifier.parse(foundStructureIdStr);
			}
		}
	}

	@Override
	protected void init() {
		setupWidgets();
	}

	@Override
	public void tick() {
		searchForNextButton.active = teleportButton.active = selectionList.hasSelection() ? selectionList.getSelected().getStructureId().equals(foundStructureId) : false;
		searchButton.active = searchForGroupButton.active = selectionList.hasSelection();

		// Check if the allowed structure list has synced
		if (AdventurersCompass.synced) {
			removeWidget(selectionList);
			allowedStructures = new ArrayList<Identifier>(AdventurersCompass.allowedStructures);
			structuresMatchingSearch = new ArrayList<Identifier>(allowedStructures);
			selectionList = addRenderableWidget(new StructureSearchList(this, minecraft, player, foundStructureId, 130, 40, width - 140, height - 50, 50));

			teleportButton.visible = AdventurersCompass.canTeleport;
			searchForNextButton.visible = AdventurersCompass.maxNextSearches > 0;
			if (searchForNextButton.visible) {
				sortByButton.setPosition(10, 125);
			} else {
				sortByButton.setPosition(10, 100);
			}
			
			AdventurersCompass.synced = false;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.centeredText(font, title, 65, 15, 0xffffffff);
	}
	
	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean ret = super.keyPressed(event);
		if (searchBox.isFocused()) {
			processSearchTerm();
			return true;
		}
		return ret;
	}
	
	@Override
	public boolean charTyped(CharacterEvent event) {
		boolean ret = super.charTyped(event);
		if (searchBox.isFocused()) {
			processSearchTerm();
			return true;
		}
		return ret;
	}

	public void selectStructure(StructureSearchEntry entry) {
		boolean enable = entry != null;
		searchButton.active = enable;
		searchForGroupButton.active = enable;
		searchForNextButton.active = enable && foundStructureId != null && entry.getStructureId().equals(foundStructureId);
		teleportButton.active = searchForNextButton.active;
	}

	public void searchForStructure(Identifier structureID) {
		ClientPlayNetworking.send(new SearchPacket(structureID, false));
		minecraft.gui.setScreen(null);
	}
	
	public void searchForGroup(Identifier groupId) {
		ClientPlayNetworking.send(new SearchPacket(groupId, true));
		minecraft.gui.setScreen(null);
	}

	public void searchForNext() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		ClientPlayNetworking.send(new SearchForNextPacket());
		minecraft.gui.setScreen(null);
	}

	public void teleport() {
		ClientPlayNetworking.send(new TeleportPacket());
		minecraft.gui.setScreen(null);
	}

	public void processSearchTerm() {
		structuresMatchingSearch = new ArrayList<Identifier>();
		String searchTerm = searchBox.getValue().toLowerCase();
		for (Identifier structureId : allowedStructures) {
			if (searchTerm.startsWith("@")) {
				if (StructureUtils.getStructureSource(structureId).toLowerCase().contains(searchTerm.substring(1))) {
					structuresMatchingSearch.add(structureId);
				}
			} else if (StructureUtils.getStructureName(structureId).toLowerCase().contains(searchTerm)) {
				structuresMatchingSearch.add(structureId);
			}
		}
		selectionList.refreshList(true);
	}

	public List<Identifier> sortStructures() {
		final List<Identifier> structures = structuresMatchingSearch;
		Collections.sort(structures, new NameSorting());
		Collections.sort(structures, sortingCategory);
		return structures;
	}

	private void setupWidgets() {
		clearWidgets();
		searchButton = addRenderableWidget(new TransparentButton(10, 40, 110, 20, Component.translatable("string.adventurerscompass.search"), (onPress) -> {
			if (selectionList.hasSelection()) {
				searchForStructure(selectionList.getSelected().getStructureId());
			}
		}));
		searchButton.active = false;
		
		searchForGroupButton = addRenderableWidget(new TransparentButton(10, 65, 110, 20, Component.translatable("string.adventurerscompass.searchForGroup"), (onPress) -> {
			if (selectionList.hasSelection()) {
				searchForGroup(selectionList.getSelected().getGroupId());
			}
		}));
		searchForGroupButton.active = false;
		
		searchForNextButton = addRenderableWidget(new TransparentButton(10, 90, 110, 20, Component.translatable("string.adventurerscompass.searchForNext"), (onPress) -> {
			searchForNext();
		}));
		searchForNextButton.visible = AdventurersCompass.maxNextSearches > 0;
		searchForNextButton.active = false;
		
		sortByButton = addRenderableWidget(new TransparentButton(10, 125, 110, 20, Component.translatable("string.adventurerscompass.sortBy").append(Component.literal(": " + sortingCategory.getLocalizedName())), (onPress) -> {
			sortingCategory = sortingCategory.next();
			sortByButton.setMessage(Component.translatable("string.adventurerscompass.sortBy").append(Component.literal(": " + sortingCategory.getLocalizedName())));
			selectionList.refreshList(true);
		}));
		if (!searchForNextButton.visible) {
			sortByButton.setPosition(10, 100);
		}
		
		teleportButton = addRenderableWidget(new TransparentButton(width - 120, 10, 110, 20, Component.translatable("string.adventurerscompass.teleport"), (onPress) -> {
			teleport();
		}));
		teleportButton.visible = AdventurersCompass.canTeleport;
		teleportButton.active = false;
		
		cancelButton = addRenderableWidget(new TransparentButton(10, height - 30, 110, 20, Component.translatable("gui.cancel"), (onPress) -> {
			minecraft.gui.setScreen(null);
		}));
		
		searchBox = addRenderableWidget(new TransparentEditBox(font, 130, 10, 140, 20, Component.translatable("string.adventurerscompass.search").withColor(0xff808080)));
		
        selectionList = addRenderableWidget(new StructureSearchList(this, minecraft, player, foundStructureId, 130, 40, width - 140, height - 50, 50));
	}

}
