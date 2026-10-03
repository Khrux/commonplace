package com.khrux.commonplace.compat;

import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class FieldGuideSyndication {
	public static Set<String> getEntries(final ServerPlayer player) {
		FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
		PlayerFieldGuideProgress progress = manager == null ? null : manager.getProgress(player);
		return progress == null ? Set.of() : new HashSet<>(progress.getUnlockedEntries());
	}

	public static void unlock(final ServerPlayer player, final Collection<String> entries) {
		FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
		PlayerFieldGuideProgress progress = manager == null ? null : manager.getProgress(player);
		if (progress == null) {
			return;
		}

		for (String entry : entries) {
			int variantStart = entry.indexOf('#');
			Identifier id = Identifier.tryParse(variantStart < 0 ? entry : entry.substring(0, variantStart));
			if (id != null && manager.isValidEntry(id)) {
				progress.unlock(player, id, variantStart < 0 ? null : entry.substring(variantStart + 1), false);
			}
		}
	}
}
