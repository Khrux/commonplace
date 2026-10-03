package com.khrux.commonplace.world.item;

import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

public class HandbookItem extends Item {
	public static final int DEFAULT_COLOR = 0xFF8C3B26;

	public HandbookItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	public static void tick(final MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isAlive() && !player.hasInfiniteMaterials()) {
				keepOne(player);
			}
		}
	}

	private static void keepOne(final ServerPlayer player) {
		ItemStack found = player.getAttachedOrCreate(CommonplaceAttachmentTypes.HANDBOOK_SLOT);
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack itemStack = inventory.getItem(i);
			if (itemStack.is(CommonplaceItems.HANDBOOK)) {
				if (!found.isEmpty()) {
					inventory.setItem(i, ItemStack.EMPTY);
				} else {
					found = itemStack;
				}
			}
		}

		found = keepOneInGrid(player.inventoryMenu, found);
		if (player.containerMenu != player.inventoryMenu && player.containerMenu instanceof AbstractCraftingMenu craftingMenu) {
			found = keepOneInGrid(craftingMenu, found);
		}

		ItemStack carried = player.containerMenu.getCarried();
		if (carried.is(CommonplaceItems.HANDBOOK)) {
			if (!found.isEmpty()) {
				player.containerMenu.setCarried(ItemStack.EMPTY);
			} else {
				found = carried;
			}
		}

		ItemStack last = player.getAttachedOrCreate(CommonplaceAttachmentTypes.LAST_HANDBOOK);
		if (found.isEmpty()) {
			player.setAttached(CommonplaceAttachmentTypes.HANDBOOK_SLOT, last.isEmpty() ? new ItemStack(CommonplaceItems.HANDBOOK) : last.copy());
		} else if (!ItemStack.isSameItemSameComponents(found, last)) {
			player.setAttached(CommonplaceAttachmentTypes.LAST_HANDBOOK, found.copyWithCount(1));
		}
	}

	public static int getColor(final Player player) {
		for (ItemStack itemStack : List.of(player.getMainHandItem(), player.getOffhandItem(), player.getAttachedOrElse(CommonplaceAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY))) {
			if (itemStack.is(CommonplaceItems.HANDBOOK)) {
				return DyedItemColor.getOrDefault(itemStack, DEFAULT_COLOR);
			}
		}

		ItemStack last = player.getAttachedOrElse(CommonplaceAttachmentTypes.LAST_HANDBOOK, ItemStack.EMPTY);
		return last.isEmpty() ? DEFAULT_COLOR : DyedItemColor.getOrDefault(last, DEFAULT_COLOR);
	}

	private static ItemStack keepOneInGrid(final AbstractCraftingMenu menu, final ItemStack alreadyFound) {
		ItemStack found = alreadyFound;
		for (Slot slot : menu.getInputGridSlots()) {
			if (slot.getItem().is(CommonplaceItems.HANDBOOK)) {
				if (!found.isEmpty()) {
					slot.set(ItemStack.EMPTY);
				} else {
					found = slot.getItem();
				}
			}
		}

		return found;
	}

	public static void entityLoad(final Entity entity, final ServerLevel level) {
		if (entity instanceof ItemEntity itemEntity && itemEntity.getItem().is(CommonplaceItems.HANDBOOK)) {
			itemEntity.discard();
		}
	}

	public static InteractionResult interact(
		final Player player, final Level level, final InteractionHand hand, final Entity entity, final @Nullable EntityHitResult hitResult
	) {
		if (player.getItemInHand(hand).is(CommonplaceItems.HANDBOOK) && (entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Allay)) {
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	public static InteractionResult useOnBlock(final Player player, final Level level, final InteractionHand hand, final BlockHitResult hitResult) {
		if (!player.getItemInHand(hand).is(CommonplaceItems.HANDBOOK)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		Block block = level.getBlockState(pos).getBlock();
		return block instanceof DecoratedPotBlock || block instanceof ShelfBlock ? InteractionResult.FAIL : InteractionResult.PASS;
	}
}
