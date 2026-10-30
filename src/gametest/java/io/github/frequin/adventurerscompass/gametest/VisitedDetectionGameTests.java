package io.github.frequin.adventurerscompass.gametest;

import java.util.List;

import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Visited detection against the real {@link StructureManager}. Vanilla
 * {@code /place structure} registers no start nor reference, so each test
 * registers a hand-made plains village Instance in the arena itself: two
 * small pieces with a gap between them, inside one start.
 */
public class VisitedDetectionGameTests {

	private static final String VILLAGE_PLAINS = "minecraft:village_plains";

	@GameTest
	public void standingInAPieceMakesTheInstanceVisited(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos firstPiece = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos gap = firstPiece.east(3);
		ChunkPos startChunk = registerVillage(level, firstPiece);
		// The mock player in the level is in creative mode, where detection is active too
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		stand(player, gap);
		VisitedDetection.poll(player);
		helper.assertTrue(PlayerVisited.get(player).list().isEmpty(), "standing between pieces, inside the start's box, should not mark the Instance");

		stand(player, firstPiece);
		VisitedDetection.poll(player);
		InstanceKey expected = new InstanceKey(level.dimension().identifier().toString(), VILLAGE_PLAINS, new ChunkCoord(startChunk.x(), startChunk.z()));
		helper.assertValueEqual(List.of(expected), PlayerVisited.get(player).list(), "Visited Instances");
		helper.succeed();
	}

	@GameTest
	public void aSpectatorMarksNothing(GameTestHelper helper) {
		BlockPos piece = helper.absolutePos(new BlockPos(1, 1, 1));
		registerVillage(helper.getLevel(), piece);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
		helper.assertTrue(player.isSpectator(), "the player should be a spectator");

		stand(player, piece);
		VisitedDetection.poll(player);

		helper.assertTrue(PlayerVisited.get(player).list().isEmpty(), "a spectator should not mark any Instance");
		helper.succeed();
	}

	private static void stand(ServerPlayer player, BlockPos pos) {
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	/**
	 * Registers a plains village start in the chunk of {@code firstPiece}, with a
	 * 1-block piece there and another 4 blocks east, and references it from every
	 * chunk its box overlaps, as worldgen would.
	 */
	private static ChunkPos registerVillage(ServerLevel level, BlockPos firstPiece) {
		Structure village = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(BuiltinStructures.VILLAGE_PLAINS);
		ChunkPos startChunk = ChunkPos.containing(firstPiece);
		BlockPos secondPiece = firstPiece.east(4);
		StructureStart start = new StructureStart(village, startChunk, 0, new PiecesContainer(List.of(piece(firstPiece), piece(secondPiece))));
		StructureManager structures = level.structureManager();
		structures.setStartForStructure(village, start, level.getChunk(startChunk.x(), startChunk.z()));
		BoundingBox box = start.getBoundingBox();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
			for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
				structures.addReferenceForStructure(village, startChunk.pack(), level.getChunk(x, z));
			}
		}
		return startChunk;
	}

	/** A piece two blocks high (feet and head) that builds nothing. */
	private static StructurePiece piece(BlockPos pos) {
		return new StructurePiece(StructurePieceType.JIGSAW, 0, new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ())) {
			@Override
			protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			}

			@Override
			public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
			}
		};
	}

}
