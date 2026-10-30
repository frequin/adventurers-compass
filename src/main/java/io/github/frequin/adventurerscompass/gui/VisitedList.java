package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.network.UnmarkVisitedPacket;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The player's Visited Instances, most recent first: a caption "Visited (N)",
 * then one row per Instance with its Structure, its distance (or its dimension
 * when elsewhere) and a red ✕. A click on a row unmarks that Instance, which
 * becomes searchable again. Reads the Visited synced to this player every frame.
 */
@Environment(EnvType.CLIENT)
public class VisitedList extends AbstractWidget {

	static final int CAPTION_HEIGHT = 12;
	private static final int ROW_HEIGHT = 11;
	private static final String CROSS = "✕";
	private static final int HOVER_COLOR = 0x40ffffff;

	private final Player player;
	private int scroll;

	public VisitedList(Player player, int x, int y, int width, int height) {
		super(x, y, width, height, Component.translatable("string.adventurerscompass.visitedList", 0));
		this.player = player;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Font font = Minecraft.getInstance().font;
		List<InstanceKey> rows = rows();
		guiGraphics.text(font, Component.translatable("string.adventurerscompass.visitedList", rows.size()), getX(), getY(), 0xffffffff);
		if (rows.isEmpty()) {
			guiGraphics.text(font, Component.translatable("string.adventurerscompass.noVisited"), getX(), listTop() + 1, 0xff808080);
			return;
		}
		scroll = Mth.clamp(scroll, 0, maxScroll(rows));
		guiGraphics.enableScissor(getX(), listTop(), getRight(), getBottom());
		InstanceKey hovered = null;
		int crossWidth = font.width(CROSS);
		for (int i = 0; i < rows.size(); i++) {
			InstanceKey key = rows.get(i);
			int top = listTop() + i * ROW_HEIGHT - scroll;
			if (top + ROW_HEIGHT < listTop() || top > getBottom()) {
				continue;
			}
			if (mouseX >= getX() && mouseX < getRight() && mouseY >= Math.max(top, listTop()) && mouseY < Math.min(top + ROW_HEIGHT, getBottom())) {
				hovered = key;
				guiGraphics.fill(getX(), top, getRight(), top + ROW_HEIGHT, HOVER_COLOR);
			}
			String where = "  " + where(key).getString();
			String name = font.plainSubstrByWidth(name(key), getWidth() - crossWidth - 4 - font.width(where));
			guiGraphics.text(font, name, getX() + 1, top + 1, 0xffffffff);
			guiGraphics.text(font, where, getX() + 1 + font.width(name), top + 1, 0xff909090);
			guiGraphics.text(font, CROSS, getRight() - crossWidth - 1, top + 1, 0xffff5555);
		}
		guiGraphics.disableScissor();
		if (hovered != null) {
			guiGraphics.setTooltipForNextFrame(font, Component.translatable("string.adventurerscompass.unmarkVisited"), mouseX, mouseY);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (event.y() < listTop()) {
			return;
		}
		int index = (int) (event.y() - listTop() + scroll) / ROW_HEIGHT;
		List<InstanceKey> rows = rows();
		if (index >= 0 && index < rows.size()) {
			ClientPlayNetworking.send(new UnmarkVisitedPacket(rows.get(index)));
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		scroll = Mth.clamp(scroll - (int) (scrollY * ROW_HEIGHT), 0, maxScroll(rows()));
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		List<String> names = rows().stream().map(VisitedList::name).toList();
		output.add(NarratedElementType.TITLE, Component.literal(Component.translatable("string.adventurerscompass.visitedList", names.size()).getString() + ": " + String.join(", ", names)));
	}

	/** Most recently Visited first. */
	private List<InstanceKey> rows() {
		List<InstanceKey> rows = new ArrayList<>(CompassTexts.visited(player).list());
		return rows.reversed();
	}

	private int listTop() {
		return getY() + CAPTION_HEIGHT;
	}

	private int maxScroll(List<InstanceKey> rows) {
		return Math.max(0, rows.size() * ROW_HEIGHT - (getBottom() - listTop()));
	}

	/** "342 m" in the Current dimension, else the dimension's name. The start chunk stands for the Instance's position. */
	private Component where(InstanceKey key) {
		if (!key.dimension().equals(CompassTexts.currentDimension(player))) {
			return CompassTexts.dimensionName(key.dimension());
		}
		BlockXZ here = new BlockXZ(player.blockPosition().getX(), player.blockPosition().getZ());
		BlockXZ start = new BlockXZ(key.startChunk().x() * 16 + 8, key.startChunk().z() * 16 + 8);
		return Component.translatable("string.adventurerscompass.visitedDistance", (int) Math.round(Math.sqrt(here.distanceSq(start))));
	}

	private static String name(InstanceKey key) {
		return StructureUtils.getStructureName(Identifier.parse(key.structureId()));
	}

}
