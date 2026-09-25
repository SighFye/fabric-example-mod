package dev.belandsigh.armorstands;

import dev.belandsigh.armorstands.ArmorStandActions.Action;
import dev.belandsigh.armorstands.ArmorStandActions.BodyPart;
import dev.belandsigh.armorstands.ArmorStandActions.Preset;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Inventory-based fallback UI for clients without the custom editor screen. */
public final class ArmorStandMenu extends ChestMenu {
	private static final int GUI_COLUMNS = 9;
	private static final int GUI_SIZE = 54;

	private final SimpleContainer controls;
	private final ServerPlayer player;
	private final ArmorStand stand;
	private final Map<Integer, Runnable> actions = new HashMap<>();
	private Page page = Page.MAIN;
	private double angleStep = 15.0;

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
		return ArmorStandActions.canEdit(player, stand);
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
		button(slot(1, 1), Items.ARMOR_STAND, "Stand settings", () -> page = Page.SETTINGS);
		button(slot(1, 3), Items.COMPARATOR, "Pose editor", () -> page = Page.POSE);
		button(slot(1, 5), Items.COMPASS, "Position and rotation", () -> page = Page.POSITION);
		button(slot(1, 7), Items.PAINTING, "Pose presets", () -> page = Page.PRESETS);
		button(slot(3, 1), Items.SPYGLASS, "Point at player", () -> page = Page.POINTING);
		button(slot(3, 3), Items.CHEST, "Copy pose", () -> execute(Action.COPY));
		button(slot(3, 5), Items.WRITABLE_BOOK, "Paste pose", () -> execute(Action.PASTE));
		button(slot(3, 7), Items.REDSTONE, "Utilities", () -> page = Page.UTILITIES);
		button(slot(5, 4), Items.BARRIER, "Close", player::closeContainer);
	}

	private void renderSettings() {
		button(slot(1, 1), Items.SMOOTH_STONE_SLAB, state("Base plate", stand.showBasePlate()), () -> execute(Action.BASE_PLATE));
		button(slot(1, 3), Items.STICK, state("Arms", stand.showArms()), () -> execute(Action.ARMS));
		button(slot(1, 5), Items.ARMOR_STAND, state("Small", stand.isSmall()), () -> execute(Action.SMALL));
		button(slot(1, 7), Items.FEATHER, state("Gravity", !stand.isNoGravity()), () -> execute(Action.GRAVITY));
		button(slot(3, 1), Items.ENDER_EYE, state("Visible", !stand.isInvisible()), () -> execute(Action.VISIBLE));
		button(slot(3, 3), Items.NAME_TAG, state("Name visible", stand.isCustomNameVisible()), () -> execute(Action.NAME_VISIBLE));
		back();
	}

	private void renderPose() {
		BodyPart[] parts = BodyPart.values();
		for (int row = 0; row < parts.length; row++) {
			BodyPart part = parts[row];
			int base = row * GUI_COLUMNS;
			label(base, Items.ARMOR_STAND, part.label());
			button(base + 1, Items.DYE.pick(DyeColor.RED), "X -" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 0, -angleStep));
			button(base + 2, Items.DYE.pick(DyeColor.LIME), "X +" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 0, angleStep));
			button(base + 3, Items.DYE.pick(DyeColor.RED), "Y -" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 1, -angleStep));
			button(base + 4, Items.DYE.pick(DyeColor.LIME), "Y +" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 1, angleStep));
			button(base + 5, Items.DYE.pick(DyeColor.RED), "Z -" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 2, -angleStep));
			button(base + 6, Items.DYE.pick(DyeColor.LIME), "Z +" + formatStep(), () -> execute(Action.ADJUST, part.ordinal(), 2, angleStep));
			if (row > 0) {
				button(base + 8, Items.BONE_MEAL, "Reset " + part.label(), () -> execute(Action.RESET_PART, part.ordinal(), 0, 0));
			}
		}
		button(slot(0, 7), Items.CLOCK, "Angle step: " + formatStep(), this::cycleAngleStep);
		button(slot(0, 8), Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void renderPosition() {
		positionRow(slot(1, 0), "X", 0);
		positionRow(slot(2, 0), "Y", 1);
		positionRow(slot(3, 0), "Z", 2);
		label(slot(4, 0), Items.COMPASS, "Yaw");
		button(slot(4, 1), Items.DYE.pick(DyeColor.RED), "Yaw -45°", () -> execute(Action.ROTATE, -45));
		button(slot(4, 2), Items.DYE.pick(DyeColor.RED), "Yaw -" + formatStep(), () -> execute(Action.ROTATE, -angleStep));
		button(slot(4, 3), Items.DYE.pick(DyeColor.LIME), "Yaw +" + formatStep(), () -> execute(Action.ROTATE, angleStep));
		button(slot(4, 4), Items.DYE.pick(DyeColor.LIME), "Yaw +45°", () -> execute(Action.ROTATE, 45));
		button(slot(4, 6), Items.ENDER_EYE, "Face player", () -> execute(Action.FACE_PLAYER));
		button(slot(4, 7), Items.TARGET, "Center on block", () -> execute(Action.CENTER));
		button(slot(4, 8), Items.CLOCK, "Angle step: " + formatStep(), this::cycleAngleStep);
		back();
	}

	private void positionRow(int base, String axisName, int axis) {
		label(base, Items.COMPASS, axisName + " position");
		button(base + 1, Items.DYE.pick(DyeColor.RED), axisName + " -0.5", () -> execute(Action.MOVE, axis, 0, -0.5));
		button(base + 2, Items.DYE.pick(DyeColor.RED), axisName + " -3/16", () -> execute(Action.MOVE, axis, 0, -0.1875));
		button(base + 3, Items.DYE.pick(DyeColor.RED), axisName + " -1/16", () -> execute(Action.MOVE, axis, 0, -0.0625));
		button(base + 4, Items.DYE.pick(DyeColor.LIME), axisName + " +1/16", () -> execute(Action.MOVE, axis, 0, 0.0625));
		button(base + 5, Items.DYE.pick(DyeColor.LIME), axisName + " +3/16", () -> execute(Action.MOVE, axis, 0, 0.1875));
		button(base + 6, Items.DYE.pick(DyeColor.LIME), axisName + " +0.5", () -> execute(Action.MOVE, axis, 0, 0.5));
	}

	private void renderPresets() {
		for (int i = 0; i < ArmorStandActions.PRESETS.size(); i++) {
			int presetIndex = i;
			Preset preset = ArmorStandActions.PRESETS.get(i);
			button(GUI_COLUMNS + i + (i / GUI_COLUMNS), Items.ARMOR_STAND, preset.name(), () -> execute(Action.PRESET, presetIndex, 0, 0));
		}
		button(slot(4, 7), Items.FIREWORK_STAR, "Random pose", () -> execute(Action.RANDOM));
		back();
	}

	private void renderUtilities() {
		button(slot(1, 1), Items.CHEST, "Copy pose", () -> execute(Action.COPY));
		button(slot(1, 3), Items.WRITABLE_BOOK, "Paste pose", () -> execute(Action.PASTE));
		button(slot(1, 5), Items.BONE_MEAL, "Reset pose", () -> execute(Action.RESET_POSE));
		button(slot(1, 7), Items.FIREWORK_STAR, "Random pose", () -> execute(Action.RANDOM));
		button(slot(3, 1), Items.STICK, "Swap main/off hands", () -> execute(Action.SWAP_HANDS));
		button(slot(3, 3), Items.IRON_HELMET, "Swap main hand/head", () -> execute(Action.SWAP_HEAD));
		button(slot(3, 5), Items.SHIELD, state("Locked", ArmorStandActions.isLocked(stand)), () -> execute(Action.LOCK));
		button(slot(3, 7), Items.BEDROCK, state("Invulnerable", stand.isPermanentlyInvulnerable()), () -> execute(Action.INVULNERABLE));
		button(slot(4, 1), Items.IRON_SWORD, "Mirror left arm to right", () -> mirror(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM));
		button(slot(4, 2), Items.IRON_SWORD, "Mirror right arm to left", () -> mirror(BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM));
		button(slot(4, 3), Items.IRON_BOOTS, "Mirror left leg to right", () -> mirror(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG));
		button(slot(4, 4), Items.IRON_BOOTS, "Mirror right leg to left", () -> mirror(BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG));
		button(slot(4, 6), Items.GLASS, "Flip entire pose", () -> execute(Action.FLIP));
		back();
	}

	private void renderPointing() {
		BodyPart[] parts = BodyPart.values();
		for (int row = 0; row < parts.length; row++) {
			BodyPart part = parts[row];
			int base = row * GUI_COLUMNS;
			label(base, Items.ARMOR_STAND, part.label());
			button(base + 2, Items.ENDER_EYE, "Point at player eyes", () -> execute(Action.POINT, part.ordinal(), 1, 0));
			button(base + 4, Items.LEATHER_BOOTS, "Point at player feet", () -> execute(Action.POINT, part.ordinal(), 0, 0));
		}
		button(slot(0, 8), Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void back() {
		button(slot(5, 4), Items.ARROW, "Back", () -> page = Page.MAIN);
	}

	private void execute(Action action) {
		execute(action, 0, 0, 0);
	}

	private void execute(Action action, double value) {
		execute(action, 0, 0, value);
	}

	private void execute(Action action, int first, int second, double value) {
		ArmorStandActions.apply(player, stand, action, first, second, value);
	}

	private void mirror(BodyPart source, BodyPart target) {
		execute(Action.MIRROR, source.ordinal(), target.ordinal(), 0);
	}

	private void cycleAngleStep() {
		angleStep = ArmorStandActions.nextAngleStep(angleStep);
	}

	private String formatStep() {
		return (int) angleStep + "°";
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

	private static int slot(int row, int column) {
		return row * GUI_COLUMNS + column;
	}

	private enum Page { MAIN, SETTINGS, POSE, POSITION, PRESETS, POINTING, UTILITIES }
}
