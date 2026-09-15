package dev.belandsigh.armorstands;

import dev.belandsigh.mixin.ArmorStandAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared, server-authoritative operations used by both armor stand editor UIs. */
public final class ArmorStandActions {
	public static final double MAX_INTERACTION_DISTANCE_SQR = 64.0;
	private static final String LOCKED_TAG = "belandsigh.locked";
	private static final String OWNER_PREFIX = "belandsigh.owner.";
	private static final Map<UUID, ArmorStand.ArmorStandPose> CLIPBOARDS = new HashMap<>();

	public static final List<Preset> PRESETS = List.of(
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
	);

	private ArmorStandActions() {
	}

	public static void apply(ServerPlayer player, ArmorStand stand, Action action, int first, int second, double value) {
		BodyPart part = BodyPart.fromNetworkId(first);
		switch (action) {
			case BASE_PLATE -> stand.setNoBasePlate(stand.showBasePlate());
			case ARMS -> stand.setShowArms(!stand.showArms());
			case SMALL -> ((ArmorStandAccessor) stand).belandsigh$setSmall(!stand.isSmall());
			case GRAVITY -> stand.setNoGravity(!stand.isNoGravity());
			case VISIBLE -> stand.setInvisible(!stand.isInvisible());
			case NAME_VISIBLE -> stand.setCustomNameVisible(!stand.isCustomNameVisible());
			case INVULNERABLE -> stand.setInvulnerable(!stand.isInvulnerable());
			case LOCK -> toggleLock(player, stand);
			case ADJUST -> {
				if (part != null && second >= 0 && second <= 2 && Math.abs(value) <= 45.0) {
					adjust(stand, part, second, (float) value);
				}
			}
			case RESET_PART -> {
				if (part != null) setPart(stand, part, part.defaultRotation());
			}
			case MOVE -> move(stand, first, Math.max(-0.5, Math.min(0.5, value)));
			case ROTATE -> stand.setYRot(stand.getYRot() + (float) Math.max(-45.0, Math.min(45.0, value)));
			case FACE_PLAYER -> facePlayer(player, stand);
			case CENTER -> stand.setPos(Math.floor(stand.getX()) + 0.5, stand.getY(), Math.floor(stand.getZ()) + 0.5);
			case PRESET -> {
				if (first >= 0 && first < PRESETS.size()) stand.setArmorStandPose(PRESETS.get(first).pose());
			}
			case RANDOM -> randomPose(stand);
			case RESET_POSE -> stand.setArmorStandPose(ArmorStand.ArmorStandPose.DEFAULT);
			case COPY -> copyPose(player, stand);
			case PASTE -> pastePose(player, stand);
			case SWAP_HANDS -> swap(stand, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
			case SWAP_HEAD -> swap(stand, EquipmentSlot.MAINHAND, EquipmentSlot.HEAD);
			case MIRROR -> {
				BodyPart target = BodyPart.fromNetworkId(second);
				if (part != null && target != null) setPart(stand, target, mirrored(getPart(stand, part)));
			}
			case FLIP -> flipPose(stand);
			case POINT -> {
				if (part != null) pointAt(player, stand, part, second == 1);
			}
			case INVALID -> { }
		}
	}

	public static boolean isLocked(ArmorStand stand) {
		return stand.entityTags().contains(LOCKED_TAG);
	}

	public static boolean isOwner(ArmorStand stand, UUID playerId) {
		return stand.entityTags().contains(OWNER_PREFIX + playerId.toString().replace("-", ""));
	}

	public static boolean isValidTarget(Player player, ArmorStand stand) {
		return stand.isAlive() && stand.level() == player.level()
				&& stand.distanceToSqr(player) <= MAX_INTERACTION_DISTANCE_SQR;
	}

	public static boolean isLockedByOther(Player player, ArmorStand stand) {
		return isLocked(stand) && !isOwner(stand, player.getUUID());
	}

	public static boolean rejectLockedInteraction(Player player, ArmorStand stand) {
		if (!isLockedByOther(player, stand)) return false;
		player.sendSystemMessage(Component.literal("That armor stand is locked."));
		return true;
	}

	public static double nextAngleStep(double current) {
		return current == 1 ? 5 : current == 5 ? 15 : current == 15 ? 45 : 1;
	}

	private static void adjust(ArmorStand stand, BodyPart part, int axis, float amount) {
		Rotations old = getPart(stand, part);
		setPart(stand, part, new Rotations(
				old.x() + (axis == 0 ? amount : 0),
				old.y() + (axis == 1 ? amount : 0),
				old.z() + (axis == 2 ? amount : 0)));
	}

	private static Rotations getPart(ArmorStand stand, BodyPart part) {
		return switch (part) {
			case HEAD -> stand.getHeadPose();
			case BODY -> stand.getBodyPose();
			case LEFT_ARM -> stand.getLeftArmPose();
			case RIGHT_ARM -> stand.getRightArmPose();
			case LEFT_LEG -> stand.getLeftLegPose();
			case RIGHT_LEG -> stand.getRightLegPose();
		};
	}

	private static void setPart(ArmorStand stand, BodyPart part, Rotations rotation) {
		switch (part) {
			case HEAD -> stand.setHeadPose(rotation);
			case BODY -> stand.setBodyPose(rotation);
			case LEFT_ARM -> stand.setLeftArmPose(rotation);
			case RIGHT_ARM -> stand.setRightArmPose(rotation);
			case LEFT_LEG -> stand.setLeftLegPose(rotation);
			case RIGHT_LEG -> stand.setRightLegPose(rotation);
		}
	}

	private static void move(ArmorStand stand, int axis, double amount) {
		if (axis == 0) stand.setPos(stand.getX() + amount, stand.getY(), stand.getZ());
		if (axis == 1) stand.setPos(stand.getX(), stand.getY() + amount, stand.getZ());
		if (axis == 2) stand.setPos(stand.getX(), stand.getY(), stand.getZ() + amount);
	}

	private static void facePlayer(ServerPlayer player, ArmorStand stand) {
		double dx = player.getX() - stand.getX();
		double dz = player.getZ() - stand.getZ();
		stand.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
	}

	private static void copyPose(ServerPlayer player, ArmorStand stand) {
		CLIPBOARDS.put(player.getUUID(), stand.getArmorStandPose());
		player.sendSystemMessage(Component.literal("Armor stand pose copied.").withStyle(ChatFormatting.GREEN));
	}

	private static void pastePose(ServerPlayer player, ArmorStand stand) {
		ArmorStand.ArmorStandPose copied = CLIPBOARDS.get(player.getUUID());
		if (copied == null) {
			player.sendSystemMessage(Component.literal("Copy a pose first.").withStyle(ChatFormatting.RED));
		} else {
			stand.setArmorStandPose(copied);
		}
	}

	private static void swap(ArmorStand stand, EquipmentSlot first, EquipmentSlot second) {
		ItemStack firstStack = stand.getItemBySlot(first).copy();
		ItemStack secondStack = stand.getItemBySlot(second).copy();
		stand.setItemSlot(first, secondStack);
		stand.setItemSlot(second, firstStack);
	}

	private static void randomPose(ArmorStand stand) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (BodyPart part : BodyPart.values()) {
			setPart(stand, part, new Rotations(random.nextInt(-180, 181), random.nextInt(-180, 181), random.nextInt(-180, 181)));
		}
	}

	private static Rotations mirrored(Rotations rotation) {
		return new Rotations(rotation.x(), -rotation.y(), -rotation.z());
	}

	private static void flipPose(ArmorStand stand) {
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

	private static void pointAt(ServerPlayer player, ArmorStand stand, BodyPart part, boolean eyes) {
		double originY = stand.getY() + (stand.isSmall() ? 0.8 : 1.45);
		double targetY = eyes ? player.getEyeY() : player.getY();
		double dx = player.getX() - stand.getX();
		double dy = targetY - originY;
		double dz = player.getZ() - stand.getZ();
		float worldYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (part != BodyPart.HEAD && part != BodyPart.BODY) pitch -= 90.0F;
		setPart(stand, part, new Rotations(pitch, worldYaw - stand.getYRot(), 0));
	}

	private static void toggleLock(ServerPlayer player, ArmorStand stand) {
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

	private static ArmorStand.ArmorStandPose pose(float hx, float hy, float hz, float bx, float by, float bz,
			float lax, float lay, float laz, float rax, float ray, float raz,
			float llx, float lly, float llz, float rlx, float rly, float rlz) {
		return new ArmorStand.ArmorStandPose(
				new Rotations(hx, hy, hz), new Rotations(bx, by, bz),
				new Rotations(lax, lay, laz), new Rotations(rax, ray, raz),
				new Rotations(llx, lly, llz), new Rotations(rlx, rly, rlz));
	}

	public enum Action {
		BASE_PLATE("base_plate"),
		ARMS("arms"),
		SMALL("small"),
		GRAVITY("gravity"),
		VISIBLE("visible"),
		NAME_VISIBLE("name_visible"),
		INVULNERABLE("invulnerable"),
		LOCK("lock"),
		ADJUST("adjust"),
		RESET_PART("reset_part"),
		MOVE("move"),
		ROTATE("rotate"),
		FACE_PLAYER("face_player"),
		CENTER("center"),
		PRESET("preset"),
		RANDOM("random"),
		RESET_POSE("reset_pose"),
		COPY("copy"),
		PASTE("paste"),
		SWAP_HANDS("swap_hands"),
		SWAP_HEAD("swap_head"),
		MIRROR("mirror"),
		FLIP("flip"),
		POINT("point"),
		INVALID("");

		private final String wireName;

		Action(String wireName) {
			this.wireName = wireName;
		}

		public String wireName() {
			return wireName;
		}

		public static Action fromWireName(String wireName) {
			for (Action action : values()) {
				if (action.wireName.equals(wireName)) return action;
			}
			return INVALID;
		}
	}

	public enum BodyPart {
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

		public String label() {
			return label;
		}

		public Rotations defaultRotation() {
			return defaultRotation;
		}

		public static BodyPart fromNetworkId(int id) {
			return id >= 0 && id < values().length ? values()[id] : null;
		}
	}

	public record Preset(String name, ArmorStand.ArmorStandPose pose) {
	}
}
