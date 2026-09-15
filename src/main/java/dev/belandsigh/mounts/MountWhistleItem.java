package dev.belandsigh.mounts;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** Reusable whistle item that delegates all gameplay decisions to server services. */
public final class MountWhistleItem extends Item {
	public MountWhistleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				MountWhistleModule.MOUNT_WHISTLE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			ServerPlayer serverPlayer = (ServerPlayer) player;
			MountCategory environment = MountEnvironmentDetector.detect(player);
			MountRecallService.RecallResult result = TargetedMountResolver.find(serverPlayer)
				.map(mount -> MountRecallService.recallTargetedMount(serverPlayer, mount, environment))
				.orElseGet(() -> MountRecallService.recallSelectedMount(serverPlayer, environment));
			player.sendOverlayMessage(result.message());
			player.getCooldowns().addCooldown(stack, MountWhistleConstants.USE_COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}
