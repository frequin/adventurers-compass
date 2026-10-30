package io.github.frequin.adventurerscompass.codec;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Minecraft codecs for the domain's Selection and Catalogue. */
public final class SelectionCodecs {

	private static final Codec<Set<String>> ID_SET = Codec.STRING.listOf().xmap(Set::copyOf, SelectionCodecs::sorted);
	private static final StreamCodec<ByteBuf, Set<String>> ID_SET_STREAM = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Set::copyOf, SelectionCodecs::sorted);

	/** Saved form of a Selection on the compass item: {@code {structures: [...], groups: [...]}}. */
	public static final Codec<Selection> SELECTION = RecordCodecBuilder.create(instance -> instance.group(
			ID_SET.optionalFieldOf("structures", Set.of()).forGetter(Selection::structures),
			ID_SET.optionalFieldOf("groups", Set.of()).forGetter(Selection::groups)).apply(instance, Selection::new));

	public static final StreamCodec<ByteBuf, Selection> SELECTION_STREAM = StreamCodec.composite(
			ID_SET_STREAM, Selection::structures,
			ID_SET_STREAM, Selection::groups,
			Selection::new);

	private static final StreamCodec<ByteBuf, CatalogueEntry> CATALOGUE_ENTRY_STREAM = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CatalogueEntry::structureId,
			ByteBufCodecs.STRING_UTF8, CatalogueEntry::groupId,
			ID_SET_STREAM, CatalogueEntry::dimensions,
			ByteBufCodecs.STRING_UTF8, CatalogueEntry::source,
			ByteBufCodecs.BOOL, CatalogueEntry::hidden,
			CatalogueEntry::new);

	public static final StreamCodec<ByteBuf, Catalogue> CATALOGUE_STREAM = CATALOGUE_ENTRY_STREAM.apply(ByteBufCodecs.list()).map(Catalogue::new, Catalogue::entries);

	private SelectionCodecs() {
	}

	private static List<String> sorted(Set<String> ids) {
		return ids.stream().sorted().toList();
	}

}
