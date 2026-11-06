package io.github.frequin.adventurerscompass.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.ReadableName;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;
import io.github.frequin.adventurerscompass.domain.selection.HiddenRule;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;

public class StructureUtils {
	
	public static Map<Identifier, Identifier> structureIdsToGroupIds(ServerLevel level) {
		Map<Identifier, Identifier> structureIdsToGroupIds = new HashMap<Identifier, Identifier>();
		if (getStructureRegistry(level).isPresent()) {
			for (Structure structure : getStructureRegistry(level).get()) {
				structureIdsToGroupIds.put(getIdForStructure(level, structure), getGroupForStructure(level, structure));
			}
		}
		return structureIdsToGroupIds;
	}
	
	/**
	 * The catalogue of every Structure, with its Group, generating dimensions,
	 * source and whether it is Hidden (common tag or {@code hiddenStructures}).
	 */
	public static Catalogue buildCatalogue(ServerLevel level) {
		final Map<Identifier, Identifier> groups = structureIdsToGroupIds(level);
		final HiddenRule hiddenRule = HiddenRule.of(AdventurersCompassConfig.hiddenStructures);
		final List<CatalogueEntry> entries = new ArrayList<CatalogueEntry>();
		for (Identifier structureId : getStructureIds(level)) {
			final Set<String> dimensions = new TreeSet<String>();
			for (Identifier dimension : getGeneratingDimensionIds(level, getStructureForId(level, structureId))) {
				dimensions.add(dimension.toString());
			}
			final boolean hidden = hiddenRule.isHidden(structureId.toString(), getTagIds(level, getStructureForId(level, structureId)));
			entries.add(new CatalogueEntry(structureId.toString(), groups.get(structureId).toString(), dimensions, getSourceName(structureId.getNamespace()), hidden));
		}
		return new Catalogue(entries);
	}

	/** Display name of the mod that owns a namespace; usable on both sides. */
	public static String getSourceName(String namespace) {
		if (namespace.equals("minecraft")) {
			return "Minecraft";
		}
		Optional<? extends ModContainer> sourceContainer = FabricLoader.getInstance().getModContainer(namespace);
		if (sourceContainer.isPresent()) {
			return sourceContainer.get().getMetadata().getName();
		}
		return namespace;
	}

	public static Identifier getGroupForStructure(ServerLevel level, Structure structure) {
		if (getStructureRegistry(level).isPresent()) {
			Registry<StructureSet> registry = getStructureSetRegistry(level).get();
			for (StructureSet set : registry) {
				for (StructureSelectionEntry entry : set.structures()) {
					if (entry.structure().value().equals(structure)) {
						return registry.getKey(set);
					}
				}
			}
		}
		return Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "none");
	}

	public static Identifier getIdForStructure(ServerLevel level, Structure structure) {
		if (getStructureRegistry(level).isPresent()) {
			return getStructureRegistry(level).get().getKey(structure);
		}
		return null;
	}

	public static Structure getStructureForId(ServerLevel level, Identifier id) {
		if (getStructureRegistry(level).isPresent()) {
			return getStructureRegistry(level).get().getValue(id);
		}
		return null;
	}

	/** Every registered Structure with a usable id, Hidden ones included. */
	public static List<Identifier> getStructureIds(ServerLevel level) {
		final List<Identifier> structureIDs = new ArrayList<Identifier>();
		if (getStructureRegistry(level).isPresent()) {
			for (Structure structure : getStructureRegistry(level).get()) {
				final Identifier id = structure == null ? null : getIdForStructure(level, structure);
				if (id != null && !id.getNamespace().isEmpty() && !id.getPath().isEmpty()) {
					structureIDs.add(id);
				}
			}
		}
		return structureIDs;
	}

	/** Ids of the tags a Structure carries, without {@code #}. */
	public static Set<String> getTagIds(ServerLevel level, Structure structure) {
		final Set<String> tags = new TreeSet<String>();
		if (getStructureRegistry(level).isPresent()) {
			getStructureRegistry(level).get().wrapAsHolder(structure).tags().forEach(tag -> tags.add(tag.location().toString()));
		}
		return tags;
	}
	
	public static List<Identifier> getGeneratingDimensionIds(ServerLevel serverLevel, Structure structure) {
		final List<Identifier> dimensions = new ArrayList<Identifier>();
		for (ServerLevel level : serverLevel.getServer().getAllLevels()) {
			ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
			Set<Holder<Biome>> biomeSet = chunkGenerator.getBiomeSource().possibleBiomes();
			if (!structure.biomes().stream().noneMatch(biomeSet::contains)) {
				dimensions.add(level.dimension().identifier());
			}
		}
		return dimensions;
	}

	@Environment(EnvType.CLIENT)
	public static String getStructureName(Identifier id) {
		if (id == null) {
			return "";
		}
		String name = id.toString();
		if (AdventurersCompassConfig.translateStructureNames) {
			name = I18n.get(Util.makeDescriptionId("structure", id));
		}
		if (name.equals(Util.makeDescriptionId("structure", id)) || !AdventurersCompassConfig.translateStructureNames) {
			name = ReadableName.of(id.toString());
		}
		return name;
	}

	/**
	 * {@code group.<namespace>.<path>} from the language files, else the name of a
	 * Structure with the same id, else the id made readable, as mods' Groups are.
	 */
	@Environment(EnvType.CLIENT)
	public static String getGroupName(Identifier id) {
		String key = Util.makeDescriptionId("group", id);
		String name = I18n.get(key);
		return AdventurersCompassConfig.translateStructureNames && !name.equals(key) ? name : getStructureName(id);
	}

	private static Optional<Registry<Structure>> getStructureRegistry(ServerLevel level) {
		return level.registryAccess().lookup(Registries.STRUCTURE);
	}
	
	private static Optional<Registry<StructureSet>> getStructureSetRegistry(ServerLevel level) {
		return level.registryAccess().lookup(Registries.STRUCTURE_SET);
	}

}