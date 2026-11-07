package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.GroupState;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.domain.selection.StructureState;
import io.github.frequin.adventurerscompass.domain.selection.TreeEntry;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.selection.TreeNames;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The left part of the compass screen: the Groups of the Current dimension,
 * each with a three-state checkbox and, once expanded, its Structures of that
 * dimension. A Group with a single Structure there is shown as that Structure.
 * Space and Enter tick the focused row; the Right and Left arrows expand and
 * collapse the focused Group.
 */
public class SelectionTree extends TransparentList<SelectionTree.Row> {

	private static final int ROW_HEIGHT = 14;
	private static final int BOX = 9;
	private static final int INDENT = 14;

	private static final TreeNames NAMES = new TreeNames() {

		@Override
		public String structure(String structureId) {
			return name(structureId);
		}

		@Override
		public String group(String groupId) {
			return groupName(groupId);
		}

	};

	private final AdventurersCompassScreen screen;
	private final Set<String> expandedGroups;
	private TreeFilter filter = TreeFilter.NONE;

	/** {@code expandedGroups} belongs to the screen, so it outlives this widget when the tabs change. */
	public SelectionTree(AdventurersCompassScreen screen, Minecraft mc, int x, int y, int width, int height, Set<String> expandedGroups) {
		super(mc, x, y, width, height, ROW_HEIGHT);
		this.screen = screen;
		this.expandedGroups = expandedGroups;
	}

	/** Narrows the tree to the search box's text or {@code @mod}; Groups are shown expanded while it is active. */
	public void setFilter(TreeFilter filter, Catalogue catalogue, String dimension) {
		this.filter = filter;
		refresh(catalogue, dimension);
		setScrollAmount(0);
	}

	/** Rebuilds the rows from the catalogue and the Selection, keeping the scroll position and the focused row. */
	public void refresh(Catalogue catalogue, String dimension) {
		double scroll = scrollAmount();
		String focused = focusedEntry() != null ? focusedEntry().key() : null;
		clearEntries();
		List<TreeEntry> tree = new ArrayList<TreeEntry>(listed(catalogue, dimension));
		tree.sort(Comparator.comparing(SelectionTree::displayName, String.CASE_INSENSITIVE_ORDER));
		for (TreeEntry entry : tree) {
			switch (entry) {
				case TreeEntry.GroupEntry group -> {
					addEntry(new GroupRow(group));
					if (isExpanded(group.groupId())) {
						group.structureIds().stream().sorted(Comparator.comparing(SelectionTree::name, String.CASE_INSENSITIVE_ORDER)).forEach(id -> addEntry(new StructureRow(id, true)));
					}
				}
				case TreeEntry.StructureEntry structure -> addEntry(new StructureRow(structure.structureId(), false));
			}
		}
		setScrollAmount(scroll);
		children().stream().filter(row -> row.key().equals(focused)).findFirst().ifPresent(this::setFocused);
	}

	/** What the tree lists, the filter included. */
	public List<TreeEntry> listed(Catalogue catalogue, String dimension) {
		return filter.apply(catalogue.tree(dimension), catalogue, NAMES);
	}

	private boolean isExpanded(String groupId) {
		return !filter.isNone() || expandedGroups.contains(groupId);
	}

	private static String displayName(TreeEntry entry) {
		return switch (entry) {
			case TreeEntry.GroupEntry group -> groupName(group.groupId());
			case TreeEntry.StructureEntry structure -> name(structure.structureId());
		};
	}

	private static String name(String id) {
		return StructureUtils.getStructureName(Identifier.parse(id));
	}

	private static String groupName(String id) {
		return StructureUtils.getGroupName(Identifier.parse(id));
	}

	private static void drawCheckbox(GuiGraphicsExtractor guiGraphics, int x, int y, Look look) {
		guiGraphics.fill(x, y, x + BOX, y + BOX, 0xffa0a0a0);
		guiGraphics.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, 0xff202020);
		switch (look) {
			case TICKED -> guiGraphics.fill(x + 2, y + 2, x + BOX - 2, y + BOX - 2, 0xff55ff55);
			case TICKED_BY_GROUP -> guiGraphics.fill(x + 2, y + 2, x + BOX - 2, y + BOX - 2, 0xff2f8f2f);
			case PARTIAL -> guiGraphics.fill(x + 2, y + 3, x + BOX - 2, y + BOX - 3, 0xff55ff55);
			case EMPTY -> {
			}
		}
	}

	private enum Look {
		TICKED, TICKED_BY_GROUP, PARTIAL, EMPTY
	}

	public abstract static class Row extends ObjectSelectionList.Entry<Row> {

		/** Finds the same row again once the tree is rebuilt. A Group and a Structure may share an id, so the kind is part of it. */
		abstract String key();

		abstract void toggleTick();

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (event.isSelection()) {
				toggleTick();
				return true;
			}
			return false;
		}

	}

	private class GroupRow extends Row {

		private final TreeEntry.GroupEntry group;

		GroupRow(TreeEntry.GroupEntry group) {
			this.group = group;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
			Minecraft mc = Minecraft.getInstance();
			int textY = getY() + (getHeight() - mc.font.lineHeight) / 2 + 1;
			int boxY = getY() + (getHeight() - BOX) / 2;
			guiGraphics.text(mc.font, isExpanded(group.groupId()) ? "-" : "+", getX() + 3, textY, 0xffc0c0c0);
			GroupState state = screen.selection().groupState(group.groupId(), screen.catalogue(), screen.dimension());
			drawCheckbox(guiGraphics, getX() + 12, boxY, switch (state) {
				case TICKED -> Look.TICKED;
				case PARTIAL -> Look.PARTIAL;
				case EMPTY -> Look.EMPTY;
			});
			String label = groupName(group.groupId());
			guiGraphics.text(mc.font, label, getX() + 12 + BOX + 4, textY, isHovering ? 0xffffffa0 : 0xffffffff);
			guiGraphics.text(mc.font, "(" + group.structureIds().size() + ")", getX() + 12 + BOX + 8 + mc.font.width(label), textY, 0xff808080);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (event.x() < getX() + 11) {
				if (!filter.isNone()) {
					// Every Group stays expanded while the filter is active
				} else if (!expandedGroups.remove(group.groupId())) {
					expandedGroups.add(group.groupId());
				}
				refresh(screen.catalogue(), screen.dimension());
			} else {
				toggleTick();
			}
			return true;
		}

		/** An arrow that changes nothing is left to move the focus, as it does everywhere else. */
		@Override
		public boolean keyPressed(KeyEvent event) {
			if (super.keyPressed(event)) {
				return true;
			}
			if (!filter.isNone()) {
				return false;
			}
			boolean changed = false;
			if (event.isRight()) {
				changed = expandedGroups.add(group.groupId());
			} else if (event.isLeft()) {
				changed = expandedGroups.remove(group.groupId());
			}
			if (changed) {
				refresh(screen.catalogue(), screen.dimension());
			}
			return changed;
		}

		@Override
		String key() {
			return "group:" + group.groupId();
		}

		@Override
		void toggleTick() {
			screen.updateSelection(screen.selection().toggleGroup(group.groupId(), screen.catalogue(), screen.dimension()));
		}

		@Override
		public Component getNarration() {
			return Component.literal(groupName(group.groupId()));
		}

	}

	private class StructureRow extends Row {

		private final String structureId;
		private final boolean indented;

		StructureRow(String structureId, boolean indented) {
			this.structureId = structureId;
			this.indented = indented;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
			Minecraft mc = Minecraft.getInstance();
			int x = getX() + 12 + (indented ? INDENT : 0);
			int textY = getY() + (getHeight() - mc.font.lineHeight) / 2 + 1;
			StructureState state = screen.selection().structureState(structureId, screen.catalogue());
			drawCheckbox(guiGraphics, x, getY() + (getHeight() - BOX) / 2, switch (state) {
				case TICKED -> Look.TICKED;
				case TICKED_BY_GROUP -> Look.TICKED_BY_GROUP;
				case EMPTY -> Look.EMPTY;
			});
			String label = name(structureId);
			guiGraphics.text(mc.font, label, x + BOX + 4, textY, isHovering ? 0xffffffa0 : 0xffffffff);
			String source = screen.catalogue().entry(structureId).map(entry -> entry.source()).orElse("");
			guiGraphics.text(mc.font, source, x + BOX + 8 + mc.font.width(label), textY, 0xff808080);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			toggleTick();
			return true;
		}

		@Override
		String key() {
			return "structure:" + structureId;
		}

		@Override
		void toggleTick() {
			screen.updateSelection(screen.selection().toggleStructure(structureId, screen.catalogue()));
		}

		@Override
		public Component getNarration() {
			return Component.literal(name(structureId));
		}

	}

}
