package io.github.frequin.adventurerscompass.sorting;

import io.github.frequin.adventurerscompass.AdventurersCompass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class GroupSorting implements ISorting {
	
	@Override
	public int compare(Identifier id1, Identifier id2) {
		return AdventurersCompass.structureIdsToGroupIds.get(id1).compareTo(AdventurersCompass.structureIdsToGroupIds.get(id2));
	}

	@Override
	public Object getValue(Identifier id) {
		return AdventurersCompass.structureIdsToGroupIds.get(id);
	}

	@Override
	public ISorting next() {
		return new NameSorting();
	}

	@Override
	public String getLocalizedName() {
		return I18n.get("string.adventurerscompass.group");
	}

}
