package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.frequin.adventurerscompass.domain.selection.Chip;
import io.github.frequin.adventurerscompass.util.StructureUtils;

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

/**
 * The Selection of the compass as chips, wrapped in rows: Groups in blue,
 * Structures in green, greyed (with a tooltip) when they do not generate in the
 * Current dimension. A click on a chip removes it from the Selection. Scrolls
 * when the chips do not fit.
 */
public class SelectionChips extends AbstractWidget {

	private static final int CHIP_HEIGHT = 12;
	private static final int GAP = 3;
	private static final int PAD = 3;
	private static final String CROSS = " ✕";

	private static final int GROUP_COLOR = 0xc0284a78;
	private static final int STRUCTURE_COLOR = 0xc0286a32;
	private static final int GREYED_COLOR = 0xc0383838;
	private static final int HOVER_BORDER = 0xffffffa0;

	private final AdventurersCompassScreen screen;
	private int scroll;

	public SelectionChips(AdventurersCompassScreen screen, int x, int y, int width, int height) {
		super(x, y, width, height, Component.translatable("string.adventurerscompass.selection"));
		this.screen = screen;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Font font = Minecraft.getInstance().font;
		List<Placed> placed = layout(font);
		if (placed.isEmpty()) {
			guiGraphics.text(font, Component.translatable("string.adventurerscompass.emptySelection"), getX(), getY() + 2, 0xff808080);
			return;
		}
		scroll = Mth.clamp(scroll, 0, maxScroll(placed));
		guiGraphics.enableScissor(getX(), getY(), getRight(), getBottom());
		Placed hovered = null;
		for (Placed chip : placed) {
			int top = chip.y - scroll;
			boolean hovering = isMouseOver(mouseX, mouseY) && mouseX >= chip.x && mouseX < chip.x + chip.width && mouseY >= top && mouseY < top + CHIP_HEIGHT;
			if (hovering) {
				hovered = chip;
				guiGraphics.fill(chip.x - 1, top - 1, chip.x + chip.width + 1, top + CHIP_HEIGHT + 1, HOVER_BORDER);
			}
			guiGraphics.fill(chip.x, top, chip.x + chip.width, top + CHIP_HEIGHT, color(chip.chip));
			int textColor = chip.chip.isActive() ? 0xffffffff : 0xff909090;
			guiGraphics.text(font, chip.label, chip.x + PAD, top + 2, textColor);
			guiGraphics.text(font, CROSS, chip.x + PAD + font.width(chip.label), top + 2, 0xffff5555);
		}
		guiGraphics.disableScissor();
		if (hovered != null) {
			guiGraphics.setComponentTooltipForNextFrame(font, tooltip(hovered.chip), mouseX, mouseY);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		for (Placed chip : layout(Minecraft.getInstance().font)) {
			int top = chip.y - scroll;
			if (event.x() >= chip.x && event.x() < chip.x + chip.width && event.y() >= top && event.y() < top + CHIP_HEIGHT) {
				screen.updateSelection(screen.selection().remove(chip.chip));
				return;
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		List<Placed> placed = layout(Minecraft.getInstance().font);
		scroll = Mth.clamp(scroll - (int) (scrollY * (CHIP_HEIGHT + GAP)), 0, maxScroll(placed));
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		List<String> names = screen.selection().chips(screen.catalogue(), screen.dimension()).stream().map(chip -> name(chip.id())).toList();
		output.add(NarratedElementType.TITLE, Component.literal(getMessage().getString() + ": " + String.join(", ", names)));
	}

	/** The chips sorted by name within each kind, placed left to right and wrapped. */
	private List<Placed> layout(Font font) {
		List<Chip> chips = new ArrayList<>(screen.selection().chips(screen.catalogue(), screen.dimension()));
		chips.sort(Comparator.comparing((Chip chip) -> chip.kind()).thenComparing(chip -> name(chip.id()), String.CASE_INSENSITIVE_ORDER));
		List<Placed> placed = new ArrayList<>();
		int x = getX();
		int y = getY();
		int crossWidth = font.width(CROSS);
		for (Chip chip : chips) {
			String label = font.plainSubstrByWidth(name(chip.id()), getWidth() - 2 * PAD - crossWidth);
			int chipWidth = font.width(label) + crossWidth + 2 * PAD;
			if (x > getX() && x + chipWidth > getRight()) {
				x = getX();
				y += CHIP_HEIGHT + GAP;
			}
			placed.add(new Placed(chip, label, x, y, chipWidth));
			x += chipWidth + GAP;
		}
		return placed;
	}

	private int maxScroll(List<Placed> placed) {
		if (placed.isEmpty()) {
			return 0;
		}
		int contentBottom = placed.get(placed.size() - 1).y + CHIP_HEIGHT;
		return Math.max(0, contentBottom - getBottom());
	}

	private static int color(Chip chip) {
		if (!chip.isActive()) {
			return GREYED_COLOR;
		}
		return chip.kind() == Chip.Kind.GROUP ? GROUP_COLOR : STRUCTURE_COLOR;
	}

	private static List<Component> tooltip(Chip chip) {
		Component kind = Component.translatable(chip.kind() == Chip.Kind.GROUP ? "string.adventurerscompass.group" : "string.adventurerscompass.structure");
		Component hint = chip.isActive()
				? Component.translatable("string.adventurerscompass.removeChip")
				: Component.translatable("string.adventurerscompass.chipAbsentFromDimension");
		return List.of(Component.empty().append(kind).append(" · " + chip.id()), hint);
	}

	private static String name(String id) {
		return StructureUtils.getStructureName(Identifier.parse(id));
	}

	private record Placed(Chip chip, String label, int x, int y, int width) {
	}

}
