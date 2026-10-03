package com.khrux.commonplace.client.compat;

import com.khrux.commonplace.world.item.CommonplaceItems;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class PunchyCompat {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String CONFIG_CLASS = "punchy.config.PunchyConfig";
	private static Method isItemBlacklisted;
	private static Method snapshot;
	private static Method applyRuntime;

	public static void bootstrap() {
		try {
			Class<?> config = Class.forName(CONFIG_CLASS);
			isItemBlacklisted = config.getMethod("isItemBlacklisted", ItemStack.class);
			snapshot = config.getMethod("snapshot");
			applyRuntime = config.getMethod("applyRuntime", snapshot.getReturnType());
		} catch (ReflectiveOperationException e) {
			warn(e);
			return;
		}

		ClientTickEvents.END_CLIENT_TICK.register(PunchyCompat::tick);
	}

	private static void tick(final Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || applyRuntime == null) {
			return;
		}

		ItemStack itemStack = player.getMainHandItem().is(CommonplaceItems.HANDBOOK) ? player.getMainHandItem() : player.getOffhandItem();
		if (!itemStack.is(CommonplaceItems.HANDBOOK)) {
			return;
		}

		try {
			if ((boolean)isItemBlacklisted.invoke(null, itemStack)) {
				return;
			}

			blacklistHandbook();
			if (!(boolean)isItemBlacklisted.invoke(null, itemStack)) {
				applyRuntime = null;
				LOGGER.warn("Punchy didn't accept the Handbook on its item blacklist; add commonplace:handbook there by hand to see the open book in first person");
			}
		} catch (ReflectiveOperationException | ClassCastException e) {
			applyRuntime = null;
			warn(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void blacklistHandbook() throws ReflectiveOperationException {
		String id = BuiltInRegistries.ITEM.getKey(CommonplaceItems.HANDBOOK).toString();
		Object data = snapshot.invoke(null);
		Field blacklistField = data.getClass().getField("itemBlacklist");
		Field dualHandedField = data.getClass().getField("blacklistApplyDualHanded");
		List<String> blacklist = new ArrayList<>((List<String>)blacklistField.get(data));
		Map<String, Boolean> dualHanded = new HashMap<>((Map<String, Boolean>)dualHandedField.get(data));
		if (!blacklist.contains(id)) {
			blacklist.add(id);
		}

		dualHanded.put(id, true);
		blacklistField.set(data, blacklist);
		dualHandedField.set(data, dualHanded);
		applyRuntime.invoke(null, data);
	}

	private static void warn(final Exception e) {
		LOGGER.warn("Couldn't add the Handbook to Punchy's item blacklist; add commonplace:handbook there by hand to see the open book in first person", e);
	}
}
