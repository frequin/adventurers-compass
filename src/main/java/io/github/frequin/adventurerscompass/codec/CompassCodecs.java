package io.github.frequin.adventurerscompass.codec;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Minecraft codecs for the compass state: Instance keys, Target, Skipped, "nothing found". */
public final class CompassCodecs {

	/** Saved form: {@code {dimension, structure, chunk_x, chunk_z}}. Also used by the Visited attachment. */
	public static final Codec<InstanceKey> INSTANCE_KEY = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(InstanceKey::dimension),
			Codec.STRING.fieldOf("structure").forGetter(InstanceKey::structureId),
			Codec.INT.fieldOf("chunk_x").forGetter(key -> key.startChunk().x()),
			Codec.INT.fieldOf("chunk_z").forGetter(key -> key.startChunk().z()))
			.apply(instance, (dimension, structure, chunkX, chunkZ) -> new InstanceKey(dimension, structure, new ChunkCoord(chunkX, chunkZ))));

	public static final StreamCodec<ByteBuf, InstanceKey> INSTANCE_KEY_STREAM = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, InstanceKey::dimension,
			ByteBufCodecs.STRING_UTF8, InstanceKey::structureId,
			ByteBufCodecs.VAR_INT, key -> key.startChunk().x(),
			ByteBufCodecs.VAR_INT, key -> key.startChunk().z(),
			(dimension, structure, chunkX, chunkZ) -> new InstanceKey(dimension, structure, new ChunkCoord(chunkX, chunkZ)));

	/** Saved form: {@code {instance: {...}, x, z}}. */
	public static final Codec<Target> TARGET = RecordCodecBuilder.create(instance -> instance.group(
			INSTANCE_KEY.fieldOf("instance").forGetter(Target::instance),
			Codec.INT.fieldOf("x").forGetter(target -> target.pos().x()),
			Codec.INT.fieldOf("z").forGetter(target -> target.pos().z()))
			.apply(instance, (key, x, z) -> new Target(key, new BlockXZ(x, z))));

	public static final StreamCodec<ByteBuf, Target> TARGET_STREAM = StreamCodec.composite(
			INSTANCE_KEY_STREAM, Target::instance,
			ByteBufCodecs.VAR_INT, target -> target.pos().x(),
			ByteBufCodecs.VAR_INT, target -> target.pos().z(),
			(key, x, z) -> new Target(key, new BlockXZ(x, z)));

	private static final Comparator<InstanceKey> ORDER = Comparator.comparing(InstanceKey::dimension)
			.thenComparing(InstanceKey::structureId)
			.thenComparingInt(key -> key.startChunk().x())
			.thenComparingInt(key -> key.startChunk().z());

	public static final Codec<Set<InstanceKey>> SKIPPED = INSTANCE_KEY.listOf().xmap(Set::copyOf, CompassCodecs::sorted);

	public static final StreamCodec<ByteBuf, Set<InstanceKey>> SKIPPED_STREAM = INSTANCE_KEY_STREAM.apply(ByteBufCodecs.list()).map(Set::copyOf, CompassCodecs::sorted);

	/** Saved form: {@code {dimension, radius, visited_ignored, skipped_ignored}}. */
	public static final Codec<SearchResult.NothingFound> NOTHING_FOUND = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(SearchResult.NothingFound::dimension),
			Codec.INT.fieldOf("radius").forGetter(SearchResult.NothingFound::radius),
			Codec.INT.fieldOf("visited_ignored").forGetter(SearchResult.NothingFound::visitedIgnored),
			Codec.INT.fieldOf("skipped_ignored").forGetter(SearchResult.NothingFound::skippedIgnored))
			.apply(instance, SearchResult.NothingFound::new));

	public static final StreamCodec<ByteBuf, SearchResult.NothingFound> NOTHING_FOUND_STREAM = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SearchResult.NothingFound::dimension,
			ByteBufCodecs.VAR_INT, SearchResult.NothingFound::radius,
			ByteBufCodecs.VAR_INT, SearchResult.NothingFound::visitedIgnored,
			ByteBufCodecs.VAR_INT, SearchResult.NothingFound::skippedIgnored,
			SearchResult.NothingFound::new);

	private CompassCodecs() {
	}

	private static List<InstanceKey> sorted(Set<InstanceKey> keys) {
		return keys.stream().sorted(ORDER).toList();
	}

}
