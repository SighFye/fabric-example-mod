package dev.belandsigh.armorstands;

import dev.belandsigh.mixin.ArmorStandAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmorStandMenu extends ChestMenu {
	private static final int GUI_SIZE = 54;
	private static final String LOCKED_TAG = "belandsigh.locked";
	private static final String OWNER_PREFIX = "belandsigh.owner.";
	private static final Map<UUID, ArmorStand.ArmorStandPose> CLIPBOARDS = new HashMap<>();
	private static final Preset[] PRESETS = {
		new Preset("Straight", pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
		new Preset("Walking", pose(0, 0, 0, 0, 0, 0, 20, 0, -10, -20, 0, 10, 20, 0, 0, -20, 0, 0)),
		new Preset("Running", pose(0, 0, 0, 0, 0, 0, 40, 0, -10, -40, 0, 10, -40, 0, 0, 40, 0, 0)),
		new Preset("Pointing", pose(0, 20, 0, 0, 0, 0, 0, 0, -10, -90, 18, 0, 0, 0, 0, 0, 0, 0)),
		new Preset("Blocking", pose(0, 0, 0, 0, 0, 0, -50, 50, 0, -20, -20, 0, 20, 0, 0, -20, 0, 0)),
		new Preset("Lunging", pose(0, 0, 0, 15, 0, 0, 10, 0, -10, -60, -10, 0, 30, 0, 0, -15, 0, 0)),
		new Preset("Winning", pose(-15, 0, 0, 0, 0, 0, 10, 0, -10, -120, -10, 0, 15, 0, 0, 0, 0, 0)),
		new Preset("Flying", pose(0, 0, 0, 0, 0, 0, -80, -20, 0, -80, 20, 0, -90, -10, 0, -90, 10, 0)),
		new Preset("Zombie", pose(-15, 0, 0, 10, 0, 0, 70, 0, -10, -140, -10, 0, 75, 0, 0, 0, 0, 0)),
		new Preset("Sitting", pose(0, 0, 0, 10, 0, 0, -75, 0, 10, -90, -10, 0, 75, 0, 0, 0, 0, 0)),
		new Preset("Confident", pose(-10, 20, 0, -2, 0, 0, 5, 0, 0, 5, 0, 0, 0, -10, -4, 16, 2, 10)),
		new Preset("Aiming", pose(0, 0, 0, 5, 0, 0, 29, 0, 25, -124, -51, -35, 0, 4, 2, 0, -4, -2)),
		new Preset("Sleeping", pose(-85, 0, 0, -90, 0, 0, -90, -10, 0, -90, 10, 0, 0, 0, 0, 0, 0, 0)),
		new Preset("Archer", pose(45, -4, 1, 10, 0, 0, -72, 24, 47, 18, -14, 0, -4, -6, -2, 25, -2, 0)),
		new Preset("Dancing", pose(14, -12, 6, 5, 0, 0, -4, -20, -10, -40, 20, 0, -88, 46, 0, -88, 71, 0)),
		new Preset("Saluting", pose(0, 30, 0, 0, 13, 0, 145, 22, -49, -22, 31, 10, -6, 0, 0, 6, -20, 0)),
		new Preset("Hugging", pose(4, 0, 0, 4, 0, 0, 30, -20, 21, 30, 22, -20, 0, 0, -5, 0, 0, 5)),
		new Preset("Thinking", pose(63, 0, 0, 10, 0, 0, -5, 0, -5, -5, 0, 5, -5, 16, -5, -5, -10, 5)),
		new Preset("T-Pose", pose(-11, 0, 0, -4, 0, 0, 0, 0, -100, 0, 0, 100, -8, 0, -60, -8, 0, 60)),
		new Preset("Facepalm", pose(-22, 25, 0, -4, 10, 0, 4, 18, 0, -153, 34, -3, 6, 24, 0, -4, 17, 2))
	};

	private final SimpleContainer controls;
	private final ServerPlayer player;
	private final ArmorStand stand;
	private final Map<Integer, Runnable> actions = new HashMap<>();
	private Page page = Page.MAIN;
	private float angleStep = 15.0F;

	public ArmorStandMenu(int containerId, Inventory inventory, ArmorStand stand) {
		this(containerId, inventory, new SimpleContainer(GUI_SIZE), stand);
	}

	private ArmorStandMenu(int containerId, Inventory inventory, SimpleContainer controls, ArmorStand stand) {
		super(MenuType.GENERIC_9x6, containerId, inventory, controls, 6);
		this.controls = controls;
		this.player = (ServerPlayer) inventory.player;
		this.stand = stand;
		render();
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player clickingPlayer) {
		if (slotIndex >= 0 && slotIndex < GUI_SIZE) {
			Runnable action = actions.get(slotIndex);
			if (action != null && validTarget()) {
				action.run();
				render();
			}
			return;
		}
		// The editor is a control surface, not storage; prevent inventory transfers while it is open.
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return validTarget();
	}

	private boolean validTarget() {
		return stand.isAlive() && stand.level() == player.level() && stand.distanceToSqr(player) <= 64.0;
	}

	private void render() {
		controls.clearContent();
		actions.clear();
		switch (page) {
			case MAIN -> renderMain();
			case SETTINGS -> renderSettings();
			case POSE -> renderPose();
			case POSITION -> renderPosition();
			case PRESETS -> renderPresets();
			case POINTING -> renderPointing();
			case UTILITIES -> renderUtilities();
		}
		broadcastChanges();
	}

	private void renderMain() {
		button(10, Items.ARMOR_STAND, "Stand settings", () -> page = Page.SETTINGS);
		button(12, Items.COMPARATOR, "Pose editor", () -> page = Page.POSE);
		button(14, Items.COMPASS, "Position and rotation", () -> page = Page.POSITION);
		button(16, Items.PAINTING, "Pose presets", () -> page = Page.PRESETS);
		button(28, Items.SPYGLASS, "Point at player", () -> page = Page.POINTING);
		button(30, Items.CHEST, "Copy pose", this::copyPose);
		button(32, Items.WRITABLE_BOOK, "Paste pose", this::pastePose);
		button(34, Items.REDSTONE, "Utilities", () -> page = Page.UTILITIES);
		button(49, Items.BARRIER, "Close", player::closeContainer);
	}

	private void renderSettings() {
		button(10, Items.SMOOTH_STONE_SLAB, state("Base plate", stand.showBasePlate()), () -> stand.setNoBasePlate(stand.showBasePlate()));
		button(12, Items.STICK, state("Arms", stand.showArms()), () -> stand.setShowArms(!stand.showArms()));
		button(14, Items.ARMOR_STAND, state("Small", stand.isSmall()), this::toggleSmall);
		button(16, Items.FEATHER, state("Gravity", !stand.isNoGravity()), () -> stand.setNoGravity(!stand.isNoGravity()));
		button(28, Items.ENDER_EYE, state("Visible", !stand.isInvisible()), () -> stand.setInvisible(!stand.isInvisible()));
		button(30, Items.NAME_TAG, state("Name visible", stand.isCustomNameVisible()), () -> stand.setCustomNameVisible(!stand.isCustomNameVisible()));
		back();
	}

	private void renderPose() {
		BodyPart[] parts = BodyPart.values();
		for (int row = 0; row < parts.length; row++) {
			BodyPart part = parts[row];
			int base = row * 9;
			label(base, Items.ARMOR_STAND, part.label);
			button(base + 1, Items.DYE.pick(DyeColor.RED), "X -" + formatStep(), () -> adjust(part, 0, -angleStep));
			button(base + 2, Items.DYE.pick(DyeColor.LIME), "X +" + formatStep(), () -> adjust(part, 0, angleStep));
			button(base + 3, Items.DYE.pick(DyeColor.RED), "Y -" + formatStep(), () -> adjust(part, 1, -angleStep));
			button(base + 4, Items.DYE.pick(DyeColor.LIME), "Y +" + formatStep(), () -> adjust(part, 1, angleStep));
			button(base + 5, Items.DYE.pick(DyeColor.RED), "Z -" + formatStep(), () -> adjust(part, 2, -angleStep));
			button(base + 6, Items.DYE.pick(DyeColor.LIME), "Z +" + formatStep(), () -> adjust(part, 2, angleStep));
			if (row > 0) {
				button(base + 8, Items.BONE_MEAL, "Reset " + part.label, () -> setPart(part, part.defaultRotation));
			}
		}
		button(7, Items.CLOCK, "Angle step: " + formatStep(), this::cycleAngleStep);
		button(8, Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void renderPosition() {
		positionRow(9, "X", 1, 0, 0);
		positionRow(18, "Y", 0, 1, 0);
		positionRow(27, "Z", 0, 0, 1);
		label(36, Items.COMPASS, "Yaw");
		button(37, Items.DYE.pick(DyeColor.RED), "Yaw -45°", () -> rotate(-45));
		button(38, Items.DYE.pick(DyeColor.RED), "Yaw -" + formatStep(), () -> rotate(-angleStep));
		button(39, Items.DYE.pick(DyeColor.LIME), "Yaw +" + formatStep(), () -> rotate(angleStep));
		button(40, Items.DYE.pick(DyeColor.LIME), "Yaw +45°", () -> rotate(45));
		button(42, Items.ENDER_EYE, "Face player", this::facePlayer);
		button(43, Items.TARGET, "Center on block", this::centerOnBlock);
		button(44, Items.CLOCK, "Angle step: " + formatStep(), this::cycleAngleStep);
		back();
	}

	private void positionRow(int base, String axis, double x, double y, double z) {
		label(base, Items.COMPASS, axis + " position");
		button(base + 1, Items.DYE.pick(DyeColor.RED), axis + " -0.5", () -> move(-0.5 * x, -0.5 * y, -0.5 * z));
		button(base + 2, Items.DYE.pick(DyeColor.RED), axis + " -3/16", () -> move(-0.1875 * x, -0.1875 * y, -0.1875 * z));
		button(base + 3, Items.DYE.pick(DyeColor.RED), axis + " -1/16", () -> move(-0.0625 * x, -0.0625 * y, -0.0625 * z));
		button(base + 4, Items.DYE.pick(DyeColor.LIME), axis + " +1/16", () -> move(0.0625 * x, 0.0625 * y, 0.0625 * z));
		button(base + 5, Items.DYE.pick(DyeColor.LIME), axis + " +3/16", () -> move(0.1875 * x, 0.1875 * y, 0.1875 * z));
		button(base + 6, Items.DYE.pick(DyeColor.LIME), axis + " +0.5", () -> move(0.5 * x, 0.5 * y, 0.5 * z));
	}

	private void renderPresets() {
		for (int i = 0; i < PRESETS.length; i++) {
			Preset preset = PRESETS[i];
			button(9 + i + (i / 9), Items.ARMOR_STAND, preset.name, () -> stand.setArmorStandPose(preset.pose));
		}
		button(43, Items.FIREWORK_STAR, "Random pose", this::randomPose);
		back();
	}

	private void renderUtilities() {
		button(10, Items.CHEST, "Copy pose", this::copyPose);
		button(12, Items.WRITABLE_BOOK, "Paste pose", this::pastePose);
		button(14, Items.BONE_MEAL, "Reset pose", () -> stand.setArmorStandPose(ArmorStand.ArmorStandPose.DEFAULT));
		button(16, Items.FIREWORK_STAR, "Random pose", this::randomPose);
		button(28, Items.STICK, "Swap main/off hands", () -> swap(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
		button(30, Items.IRON_HELMET, "Swap main hand/head", () -> swap(EquipmentSlot.MAINHAND, EquipmentSlot.HEAD));
		button(32, Items.SHIELD, state("Locked", isLocked(stand)), this::toggleLock);
		button(34, Items.BEDROCK, state("Invulnerable", stand.isInvulnerable()), () -> stand.setInvulnerable(!stand.isInvulnerable()));
		button(37, Items.IRON_SWORD, "Mirror left arm to right", () -> mirrorPart(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM));
		button(38, Items.IRON_SWORD, "Mirror right arm to left", () -> mirrorPart(BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM));
		button(39, Items.IRON_BOOTS, "Mirror left leg to right", () -> mirrorPart(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG));
		button(40, Items.IRON_BOOTS, "Mirror right leg to left", () -> mirrorPart(BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG));
		button(42, Items.GLASS, "Flip entire pose", this::flipPose);
		back();
	}

	private void renderPointing() {
		BodyPart[] parts = BodyPart.values();
		for (int row = 0; row < parts.length; row++) {
			BodyPart part = parts[row];
			int base = row * 9;
			label(base, Items.ARMOR_STAND, part.label);
			button(base + 2, Items.ENDER_EYE, "Point at player eyes", () -> pointAt(part, true));
			button(base + 4, Items.LEATHER_BOOTS, "Point at player feet", () -> pointAt(part, false));
		}
		button(8, Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void back() {
		button(49, Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void toggleSmall() {
		((ArmorStandAccessor) stand).belandsigh$setSmall(!stand.isSmall());
	}

	private void move(double x, double y, double z) {
		stand.setPos(stand.getX() + x, stand.getY() + y, stand.getZ() + z);
	}

	private void rotate(float amount) {
		stand.setYRot(stand.getYRot() + amount);
	}

	private void centerOnBlock() {
		stand.setPos(Math.floor(stand.getX()) + 0.5, stand.getY(), Math.floor(stand.getZ()) + 0.5);
	}

	private void facePlayer() {
		double dx = player.getX() - stand.getX();
		double dz = player.getZ() - stand.getZ();
		stand.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
	}

	private void cycleAngleStep() {
		angleStep = angleStep == 1 ? 5 : angleStep == 5 ? 15 : angleStep == 15 ? 45 : 1;
	}

	private String formatStep() {
		return (int) angleStep + "°";
	}

	private void adjust(BodyPart part, int axis, float amount) {
		Rotations old = getPart(part);
		float x = old.x();
		float y = old.y();
		float z = old.z();
		if (axis == 0) x += amount;
		if (axis == 1) y += amount;
		if (axis == 2) z += amount;
		setPart(part, new Rotations(x, y, z));
	}

	private Rotations getPart(BodyPart part) {
		return switch (part) {
			case HEAD -> stand.getHeadPose();
			case BODY -> stand.getBodyPose();
			case LEFT_ARM -> stand.getLeftArmPose();
			case RIGHT_ARM -> stand.getRightArmPose();
			case LEFT_LEG -> stand.getLeftLegPose();
			case RIGHT_LEG -> stand.getRightLegPose();
		};
	}

	private void setPart(BodyPart part, Rotations rotation) {
		switch (part) {
			case HEAD -> stand.setHeadPose(rotation);
			case BODY -> stand.setBodyPose(rotation);
			case LEFT_ARM -> stand.setLeftArmPose(rotation);
			case RIGHT_ARM -> stand.setRightArmPose(rotation);
			case LEFT_LEG -> stand.setLeftLegPose(rotation);
			case RIGHT_LEG -> stand.setRightLegPose(rotation);
		}
	}

	private void copyPose() {
		CLIPBOARDS.put(player.getUUID(), stand.getArmorStandPose());
		player.sendSystemMessage(Component.literal("Armor stand pose copied.").withStyle(ChatFormatting.GREEN));
	}

	private void pastePose() {
		ArmorStand.ArmorStandPose copied = CLIPBOARDS.get(player.getUUID());
		if (copied == null) {
			player.sendSystemMessage(Component.literal("Copy a pose first.").withStyle(ChatFormatting.RED));
			return;
		}
		stand.setArmorStandPose(copied);
	}

	private void swap(EquipmentSlot first, EquipmentSlot second) {
		ItemStack firstStack = stand.getItemBySlot(first).copy();
		ItemStack secondStack = stand.getItemBySlot(second).copy();
		stand.setItemSlot(first, secondStack);
		stand.setItemSlot(second, firstStack);
	}

	private void randomPose() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (BodyPart part : BodyPart.values()) {
			setPart(part, new Rotations(random.nextInt(-180, 181), random.nextInt(-180, 181), random.nextInt(-180, 181)));
		}
	}

	private void mirrorPart(BodyPart source, BodyPart target) {
		Rotations rotation = getPart(source);
		setPart(target, mirrored(rotation));
	}

	private void flipPose() {
		Rotations leftArm = stand.getLeftArmPose();
		Rotations rightArm = stand.getRightArmPose();
		Rotations leftLeg = stand.getLeftLegPose();
		Rotations rightLeg = stand.getRightLegPose();
		stand.setLeftArmPose(mirrored(rightArm));
		stand.setRightArmPose(mirrored(leftArm));
		stand.setLeftLegPose(mirrored(rightLeg));
		stand.setRightLegPose(mirrored(leftLeg));
		Rotations head = stand.getHeadPose();
		Rotations body = stand.getBodyPose();
		stand.setHeadPose(new Rotations(head.x(), -head.y(), -head.z()));
		stand.setBodyPose(new Rotations(body.x(), -body.y(), -body.z()));
	}

	private Rotations mirrored(Rotations rotation) {
		return new Rotations(rotation.x(), -rotation.y(), -rotation.z());
	}

	private void pointAt(BodyPart part, boolean eyes) {
		double originY = stand.getY() + (stand.isSmall() ? 0.8 : 1.45);
		double targetY = eyes ? player.getEyeY() : player.getY();
		double dx = player.getX() - stand.getX();
		double dy = targetY - originY;
		double dz = player.getZ() - stand.getZ();
		float worldYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float localYaw = worldYaw - stand.getYRot();
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (part != BodyPart.HEAD && part != BodyPart.BODY) {
			pitch -= 90.0F;
		}
		setPart(part, new Rotations(pitch, localYaw, 0));
	}

	private void toggleLock() {
		if (isLocked(stand)) {
			if (!isOwner(stand, player.getUUID())) {
				player.sendSystemMessage(Component.literal("Only the owner can unlock this stand.").withStyle(ChatFormatting.RED));
				return;
			}
			stand.removeTag(LOCKED_TAG);
			stand.entityTags().stream().filter(tag -> tag.startsWith(OWNER_PREFIX)).toList().forEach(stand::removeTag);
			return;
		}
		stand.addTag(LOCKED_TAG);
		stand.addTag(OWNER_PREFIX + player.getUUID().toString().replace("-", ""));
	}

	static boolean isLocked(ArmorStand stand) {
		return stand.entityTags().contains(LOCKED_TAG);
	}

	static boolean isOwner(ArmorStand stand, UUID playerId) {
		return stand.entityTags().contains(OWNER_PREFIX + playerId.toString().replace("-", ""));
	}

	public static void applyNetworkAction(ServerPlayer player, ArmorStand stand,
			ArmorStandNetwork.ActionPayload payload) {
		BodyPart part = payload.first() >= 0 && payload.first() < BodyPart.values().length
				? BodyPart.values()[payload.first()] : null;
		switch (payload.action()) {
			case "base_plate" -> stand.setNoBasePlate(stand.showBasePlate());
			case "arms" -> stand.setShowArms(!stand.showArms());
			case "small" -> ((ArmorStandAccessor) stand).belandsigh$setSmall(!stand.isSmall());
			case "gravity" -> stand.setNoGravity(!stand.isNoGravity());
			case "visible" -> stand.setInvisible(!stand.isInvisible());
			case "name_visible" -> stand.setCustomNameVisible(!stand.isCustomNameVisible());
			case "invulnerable" -> stand.setInvulnerable(!stand.isInvulnerable());
			case "lock" -> toggleNetworkLock(player, stand);
			case "adjust" -> {
				if (part != null && payload.second() >= 0 && payload.second() <= 2
						&& Math.abs(payload.value()) <= 45.0) {
					Rotations old = getNetworkPart(stand, part);
					float x = old.x();
					float y = old.y();
					float z = old.z();
					if (payload.second() == 0) x += (float) payload.value();
					if (payload.second() == 1) y += (float) payload.value();
					if (payload.second() == 2) z += (float) payload.value();
					setNetworkPart(stand, part, new Rotations(x, y, z));
				}
			}
			case "reset_part" -> {
				if (part != null) setNetworkPart(stand, part, part.defaultRotation);
			}
			case "move" -> {
				double amount = Math.max(-0.5, Math.min(0.5, payload.value()));
				if (payload.first() == 0) stand.setPos(stand.getX() + amount, stand.getY(), stand.getZ());
				if (payload.first() == 1) stand.setPos(stand.getX(), stand.getY() + amount, stand.getZ());
				if (payload.first() == 2) stand.setPos(stand.getX(), stand.getY(), stand.getZ() + amount);
			}
			case "rotate" -> stand.setYRot(stand.getYRot() + (float) Math.max(-45.0, Math.min(45.0, payload.value())));
			case "face_player" -> {
				double dx = player.getX() - stand.getX();
				double dz = player.getZ() - stand.getZ();
				stand.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
			}
			case "center" -> stand.setPos(Math.floor(stand.getX()) + 0.5, stand.getY(), Math.floor(stand.getZ()) + 0.5);
			case "preset" -> {
				if (payload.first() >= 0 && payload.first() < PRESETS.length) {
					stand.setArmorStandPose(PRESETS[payload.first()].pose);
				}
			}
			case "random" -> {
				ThreadLocalRandom random = ThreadLocalRandom.current();
				for (BodyPart bodyPart : BodyPart.values()) {
					setNetworkPart(stand, bodyPart, new Rotations(random.nextInt(-180, 181),
							random.nextInt(-180, 181), random.nextInt(-180, 181)));
				}
			}
			case "reset_pose" -> stand.setArmorStandPose(ArmorStand.ArmorStandPose.DEFAULT);
			case "copy" -> {
				CLIPBOARDS.put(player.getUUID(), stand.getArmorStandPose());
				player.sendSystemMessage(Component.literal("Armor stand pose copied.").withStyle(ChatFormatting.GREEN));
			}
			case "paste" -> {
				ArmorStand.ArmorStandPose copied = CLIPBOARDS.get(player.getUUID());
				if (copied == null) {
					player.sendSystemMessage(Component.literal("Copy a pose first.").withStyle(ChatFormatting.RED));
				} else {
					stand.setArmorStandPose(copied);
				}
			}
			case "swap_hands" -> swapNetwork(stand, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
			case "swap_head" -> swapNetwork(stand, EquipmentSlot.MAINHAND, EquipmentSlot.HEAD);
			case "mirror" -> {
				BodyPart target = payload.second() >= 0 && payload.second() < BodyPart.values().length
						? BodyPart.values()[payload.second()] : null;
				if (part != null && target != null) setNetworkPart(stand, target, mirroredNetwork(getNetworkPart(stand, part)));
			}
			case "flip" -> flipNetwork(stand);
			case "point" -> {
				if (part != null) pointNetwork(player, stand, part, payload.second() == 1);
			}
			default -> { }
		}
	}

	private static Rotations getNetworkPart(ArmorStand stand, BodyPart part) {
		return switch (part) {
			case HEAD -> stand.getHeadPose();
			case BODY -> stand.getBodyPose();
			case LEFT_ARM -> stand.getLeftArmPose();
			case RIGHT_ARM -> stand.getRightArmPose();
			case LEFT_LEG -> stand.getLeftLegPose();
			case RIGHT_LEG -> stand.getRightLegPose();
		};
	}

	private static void setNetworkPart(ArmorStand stand, BodyPart part, Rotations rotation) {
		switch (part) {
			case HEAD -> stand.setHeadPose(rotation);
			case BODY -> stand.setBodyPose(rotation);
			case LEFT_ARM -> stand.setLeftArmPose(rotation);
			case RIGHT_ARM -> stand.setRightArmPose(rotation);
			case LEFT_LEG -> stand.setLeftLegPose(rotation);
			case RIGHT_LEG -> stand.setRightLegPose(rotation);
		}
	}

	private static void swapNetwork(ArmorStand stand, EquipmentSlot first, EquipmentSlot second) {
		ItemStack firstStack = stand.getItemBySlot(first).copy();
		ItemStack secondStack = stand.getItemBySlot(second).copy();
		stand.setItemSlot(first, secondStack);
		stand.setItemSlot(second, firstStack);
	}

	private static Rotations mirroredNetwork(Rotations rotation) {
		return new Rotations(rotation.x(), -rotation.y(), -rotation.z());
	}

	private static void flipNetwork(ArmorStand stand) {
		Rotations leftArm = stand.getLeftArmPose();
		Rotations rightArm = stand.getRightArmPose();
		Rotations leftLeg = stand.getLeftLegPose();
		Rotations rightLeg = stand.getRightLegPose();
		stand.setLeftArmPose(mirroredNetwork(rightArm));
		stand.setRightArmPose(mirroredNetwork(leftArm));
		stand.setLeftLegPose(mirroredNetwork(rightLeg));
		stand.setRightLegPose(mirroredNetwork(leftLeg));
		Rotations head = stand.getHeadPose();
		Rotations body = stand.getBodyPose();
		stand.setHeadPose(new Rotations(head.x(), -head.y(), -head.z()));
		stand.setBodyPose(new Rotations(body.x(), -body.y(), -body.z()));
	}

	private static void pointNetwork(ServerPlayer player, ArmorStand stand, BodyPart part, boolean eyes) {
		double originY = stand.getY() + (stand.isSmall() ? 0.8 : 1.45);
		double targetY = eyes ? player.getEyeY() : player.getY();
		double dx = player.getX() - stand.getX();
		double dy = targetY - originY;
		double dz = player.getZ() - stand.getZ();
		float worldYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float localYaw = worldYaw - stand.getYRot();
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (part != BodyPart.HEAD && part != BodyPart.BODY) pitch -= 90.0F;
		setNetworkPart(stand, part, new Rotations(pitch, localYaw, 0));
	}

	private static void toggleNetworkLock(ServerPlayer player, ArmorStand stand) {
		if (isLocked(stand)) {
			stand.removeTag(LOCKED_TAG);
			stand.entityTags().stream().filter(tag -> tag.startsWith(OWNER_PREFIX)).toList().forEach(stand::removeTag);
		} else {
			stand.addTag(LOCKED_TAG);
			stand.addTag(OWNER_PREFIX + player.getUUID().toString().replace("-", ""));
		}
	}

	private void button(int slot, Item item, String name, Runnable action) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(ChatFormatting.YELLOW));
		controls.setItem(slot, stack);
		actions.put(slot, action);
	}

	private void label(int slot, Item item, String name) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(ChatFormatting.AQUA));
		controls.setItem(slot, stack);
	}

	private static String state(String label, boolean enabled) {
		return label + ": " + (enabled ? "ON" : "OFF");
	}

	private static ArmorStand.ArmorStandPose pose(float hx, float hy, float hz, float bx, float by, float bz,
			float lax, float lay, float laz, float rax, float ray, float raz,
			float llx, float lly, float llz, float rlx, float rly, float rlz) {
		return new ArmorStand.ArmorStandPose(
				new Rotations(hx, hy, hz), new Rotations(bx, by, bz),
				new Rotations(lax, lay, laz), new Rotations(rax, ray, raz),
				new Rotations(llx, lly, llz), new Rotations(rlx, rly, rlz));
	}

	private enum Page { MAIN, SETTINGS, POSE, POSITION, PRESETS, POINTING, UTILITIES }

	private enum BodyPart {
		HEAD("Head", ArmorStand.DEFAULT_HEAD_POSE),
		BODY("Body", ArmorStand.DEFAULT_BODY_POSE),
		LEFT_ARM("Left arm", ArmorStand.DEFAULT_LEFT_ARM_POSE),
		RIGHT_ARM("Right arm", ArmorStand.DEFAULT_RIGHT_ARM_POSE),
		LEFT_LEG("Left leg", ArmorStand.DEFAULT_LEFT_LEG_POSE),
		RIGHT_LEG("Right leg", ArmorStand.DEFAULT_RIGHT_LEG_POSE);

		private final String label;
		private final Rotations defaultRotation;

		BodyPart(String label, Rotations defaultRotation) {
			this.label = label;
			this.defaultRotation = defaultRotation;
		}
	}

	private record Preset(String name, ArmorStand.ArmorStandPose pose) {
	}
}
