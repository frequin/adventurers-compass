package io.github.frequin.adventurerscompass.worker;

import java.util.List;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public abstract class StructureSearchWorker<T extends StructurePlacement> implements WorldWorkerManager.IWorker {
	
	protected String managerId;
	protected ServerLevel level;
	protected Player player;
	protected ItemStack stack;
	protected BlockPos startPos;
	protected BlockPos currentPos;
	protected T placement;
	protected List<Structure> structureSet;
	protected Identifier structureOrGroupId;
	protected boolean isGroup;
	protected int samples;
	protected boolean finished;
	protected int lastRadiusThreshold;
	protected List<BlockPos> prevPos;

	public StructureSearchWorker(ServerLevel level, Player player, ItemStack stack, BlockPos startPos, List<BlockPos> prevPos, T placement, List<Structure> structureSet, Identifier structureOrGroupId, boolean isGroup, String managerId) {
		this.level = level;
		this.player = player;
		this.stack = stack;
		this.startPos = startPos;
		this.prevPos = prevPos;
		this.structureSet = structureSet;
		this.structureOrGroupId = structureOrGroupId;
		this.isGroup = isGroup;
		this.placement = placement;
		this.managerId = managerId;

		currentPos = startPos;
		samples = 0;

		finished = /*!level.getServer().getWorldData().worldGenOptions().generateStructures()*/ false;
	}

	public void start() {
		if (!stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM) {
			if (AdventurersCompassConfig.maxRadius > 0) {
				AdventurersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " starting with " + (shouldLogRadius() ? AdventurersCompassConfig.maxRadius + " max radius, " : "") + AdventurersCompassConfig.maxSamples + " max samples, " + prevPos.size() + " previous locations");
				WorldWorkerManager.addWorker(this);
			} else {
				fail();
			}
		}
	}

	@Override
	public boolean hasWork() {
		return !finished && prevPos.size() <= AdventurersCompassConfig.maxNextSearches && getRadius() < AdventurersCompassConfig.maxRadius && samples < AdventurersCompassConfig.maxSamples;
	}

	@Override
	public boolean doWork() {
		int radius = getRadius();
		if (radius > 250 && radius / 250 > lastRadiusThreshold) {
			if (!stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM) {
				stack.set(AdventurersCompass.SEARCH_RADIUS, roundRadius(radius, 250));
			}
			lastRadiusThreshold = radius / 250;
		}
		return false;
	}

	protected Pair<BlockPos, Structure> getStructureGeneratingAt(ChunkPos chunkPos) {
		for (Structure structure : structureSet) {
			StructureCheckResult result = level.structureManager().checkStructurePresence(chunkPos, structure, placement, false);
			if (result != StructureCheckResult.START_NOT_PRESENT) {
				if (result == StructureCheckResult.START_PRESENT) {
					return Pair.of(placement.getLocatePos(chunkPos), structure);
				}

				ChunkAccess chunkAccess = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.STRUCTURE_STARTS);
				StructureStart structureStart = level.structureManager().getStartForStructure(structure, chunkAccess);
				if (structureStart != null && structureStart.isValid()) {
					return Pair.of(placement.getLocatePos(structureStart.getChunkPos()), structure);
				}
			}
		}

		return null;
	}

	protected void succeed(BlockPos pos, Structure structure) {
		AdventurersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " succeeded with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		if (!stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM) {
			((AdventurersCompassItem) stack.getItem()).succeed(player, stack, StructureUtils.getIdForStructure(level, structure), isGroup, pos.getX(), pos.getZ(), prevPos, samples, AdventurersCompassConfig.displayCoordinates);
		} else {
			AdventurersCompass.LOGGER.error("SearchWorkerManager " + managerId + ": " + getName() + " found invalid compass after successful search");
		}
		finished = true;
	}

	public boolean shouldIgnore(BlockPos pos) {
		return prevPos.contains(pos);
	}

	protected void fail() {
		AdventurersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " failed with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		if (!stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM) {
			((AdventurersCompassItem) stack.getItem()).fail(stack, structureOrGroupId, roundRadius(getRadius(), 250), samples);
		} else {
			AdventurersCompass.LOGGER.error("SearchWorkerManager " + managerId + ": " + getName() + " found invalid compass after failed search");
		}
		finished = true;
	}

	public void stop() {
		AdventurersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " stopped with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		finished = true;
	}

	protected int getRadius() {
		return StructureUtils.getHorizontalDistanceToLocation(startPos, currentPos.getX(), currentPos.getZ());
	}

	protected int roundRadius(int radius, int roundTo) {
		return ((int) radius / roundTo) * roundTo;
	}
	
	protected abstract String getName();
	
	protected abstract boolean shouldLogRadius();

}