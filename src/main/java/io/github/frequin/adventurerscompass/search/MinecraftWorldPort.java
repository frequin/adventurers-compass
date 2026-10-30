package io.github.frequin.adventurerscompass.search;

import java.util.List;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.search.CandidateSource;
import io.github.frequin.adventurerscompass.domain.search.ChunkSpiralCandidates;
import io.github.frequin.adventurerscompass.domain.search.FixedCandidates;
import io.github.frequin.adventurerscompass.domain.search.RandomSpreadCandidates;
import io.github.frequin.adventurerscompass.domain.search.WorldPort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.DimensionOriginStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * The search's world port, backed by the server's chunk generators. Must be
 * used on the server thread: presence tests can generate chunks up to
 * structure starts.
 */
public final class MinecraftWorldPort implements WorldPort<StructurePlacement> {

	private final MinecraftServer server;

	public MinecraftWorldPort(MinecraftServer server) {
		this.server = server;
	}

	@Override
	public List<StructurePlacement> placements(String dimension, String structureId) {
		ServerLevel level = level(dimension);
		Structure structure = structure(level, structureId);
		if (level == null || structure == null) {
			return List.of();
		}
		var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(structure);
		return level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder);
	}

	@Override
	public CandidateSource candidates(String dimension, StructurePlacement placement, BlockXZ origin) {
		ChunkGeneratorStructureState state = level(dimension).getChunkSource().getGeneratorState();
		if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
			int spacing = randomSpread.spacing();
			long seed = state.getLevelSeed();
			return new RandomSpreadCandidates(origin, spacing, (cellX, cellZ) -> toCoord(randomSpread.getPotentialStructureChunk(seed, cellX * spacing, cellZ * spacing)), chunk -> locatePos(placement, chunk));
		}
		if (placement instanceof ConcentricRingsStructurePlacement rings) {
			List<ChunkPos> positions = state.getRingPositionsFor(rings);
			return new FixedCandidates(origin, positions == null ? List.of() : positions.stream().map(MinecraftWorldPort::toCoord).toList(), chunk -> locatePos(placement, chunk));
		}
		if (placement instanceof DimensionOriginStructurePlacement) {
			return new FixedCandidates(origin, List.of(toCoord(state.getDimensionOrigin())), chunk -> locatePos(placement, chunk));
		}
		return new ChunkSpiralCandidates(origin, chunk -> locatePos(placement, chunk));
	}

	@Override
	public boolean isPresent(String dimension, String structureId, StructurePlacement placement, ChunkCoord candidate) {
		ServerLevel level = level(dimension);
		Structure structure = structure(level, structureId);
		if (level == null || structure == null) {
			return false;
		}
		// Cheap placement rules first (frequency, exclusion zones): vanilla's own
		// presence check scans region files before applying them.
		if (!placement.isStructureChunk(level.getChunkSource().getGeneratorState(), candidate.x(), candidate.z())) {
			return false;
		}
		ChunkPos chunkPos = new ChunkPos(candidate.x(), candidate.z());
		StructureCheckResult result = level.structureManager().checkStructurePresence(chunkPos, structure, placement, false);
		if (result == StructureCheckResult.START_PRESENT) {
			return true;
		}
		if (result == StructureCheckResult.START_NOT_PRESENT) {
			return false;
		}
		ChunkAccess chunk = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.STRUCTURE_STARTS);
		StructureStart start = level.structureManager().getStartForStructure(structure, chunk);
		return start != null && start.isValid();
	}

	private ServerLevel level(String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	private static Structure structure(ServerLevel level, String structureId) {
		Identifier id = Identifier.tryParse(structureId);
		if (level == null || id == null) {
			return null;
		}
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(id);
	}

	private static BlockXZ locatePos(StructurePlacement placement, ChunkCoord chunk) {
		BlockPos pos = placement.getLocatePos(new ChunkPos(chunk.x(), chunk.z()));
		return new BlockXZ(pos.getX(), pos.getZ());
	}

	private static ChunkCoord toCoord(ChunkPos chunkPos) {
		return new ChunkCoord(chunkPos.x(), chunkPos.z());
	}

}
