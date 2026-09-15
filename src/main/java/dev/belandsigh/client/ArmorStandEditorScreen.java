package dev.belandsigh.client;

import dev.belandsigh.armorstands.ArmorStandActions;
import dev.belandsigh.armorstands.ArmorStandActions.Action;
import dev.belandsigh.armorstands.ArmorStandActions.BodyPart;
import dev.belandsigh.armorstands.ArmorStandNetwork.ActionPayload;
import dev.belandsigh.armorstands.ArmorStandNetwork.StatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorStandEditorScreen extends Screen {
	private static ArmorStandEditorScreen current;
	private static final String[] TAB_NAMES = {"Stand", "Pose", "Position", "Presets", "Tools"};
	private static final int PANEL_MARGIN = 10;
	private static final int PANEL_MIN_WIDTH = 150;
	private static final int PANEL_MAX_WIDTH = 300;
	private static final int CENTER_RESERVED_WIDTH = 200;
	private static final int PANEL_INSET = 8;
	private static final int TAB_Y_OFFSET = 28;
	private static final int TAB_GAP = 2;
	private static final int LEFT_TAB_COUNT = 3;
	private static final int PARTS_PER_PANEL = 3;
	private static final int POSE_LABEL_Y_OFFSET = 78;
	private static final int POSE_ROW_SPACING = 47;
	private static final int POSE_CONTROL_Y_OFFSET = 17;
	private static final int SECONDARY_ROW_Y_OFFSET = 55;
	private static final int CONTROL_HEIGHT = 20;
	private static final int CLOSE_BUTTON_INSET = 28;
	private static final int CLOSE_BUTTON_Y = 5;
	private static final int CLOSE_BUTTON_WIDTH = 20;
	private static final int COMPACT_BUTTON_HEIGHT = 18;
	private static final int POSE_MODE_BUTTON_X_OFFSET = 10;
	private static final int POSE_MODE_BUTTON_WIDTH = 82;
	private static final int POSE_MODE_BUTTON_GAP = 4;
	private static final int STEP_BUTTON_X_INSET = 105;
	private static final int STEP_BUTTON_WIDTH = 95;
	private static final int CHECKBOX_ROW_Y_OFFSET = 68;
	private static final int CHECKBOX_COLUMN_X_OFFSET = 20;
	private static final int CHECKBOX_ROW_SPACING = 30;
	private static final int AXES_ON_LEFT_PANEL = 2;
	private static final int POSITION_ROW_Y_OFFSET = 83;
	private static final int POSITION_ROW_SPACING = 54;
	private static final int POSITION_LABEL_Y_OFFSET = 70;
	private static final int SECONDARY_BUTTON_HEIGHT = 22;
	private static final int YAW_ROW_Y_OFFSET = 137;
	private static final int FACE_CENTER_Y_OFFSET = 170;
	private static final int PRESETS_PER_PANEL = 10;
	private static final int PRESET_ROW_Y_OFFSET = 62;
	private static final int PRESET_ROW_SPACING = 30;
	private static final int ACTION_BUTTON_HEIGHT = 24;
	private static final int RANDOM_POSE_Y_OFFSET = 207;
	private static final int RANDOM_POSE_WIDTH = 116;
	private static final int TOOLS_ROW_Y_OFFSET = 69;
	private static final int TOOLS_ROW_SPACING = 32;
	private static final int TOOLS_BUTTON_X_INSET = 18;
	private static final int TOOLS_BUTTON_WIDTH_MARGIN = 36;

	private final int entityId;
	private StatePayload state;
	private int tab;
	private boolean pointingMode;
	private double angleStep = 15.0;
	private int left;
	private int right;
	private int top;
	private int sideWidth;
	private int panelHeight;

	public ArmorStandEditorScreen(int entityId, StatePayload state) {
		super(Component.literal("Armour Stand Editor"));
		this.entityId = entityId;
		this.state = state;
		current = this;
	}

	public static void updateCurrentState(StatePayload state) {
		if (current != null) {
			current.state = state;
			current.rebuildWidgets();
		}
	}

	@Override
	public void removed() {
		if (current == this) current = null;
		super.removed();
	}

	@Override
	protected void init() {
		sideWidth = Math.min(PANEL_MAX_WIDTH, Math.max(PANEL_MIN_WIDTH, (width - CENTER_RESERVED_WIDTH) / 2));
		left = PANEL_MARGIN;
		right = width - sideWidth - PANEL_MARGIN;
		top = PANEL_MARGIN;
		panelHeight = height - PANEL_MARGIN * 2;
		buildTabs();
		switch (tab) {
			case 0 -> buildStandTab();
			case 1 -> buildPoseTab();
			case 2 -> buildPositionTab();
			case 3 -> buildPresetsTab();
			case 4 -> buildToolsTab();
			default -> { }
		}
	}

	private void buildTabs() {
		int leftTabWidth = (sideWidth - PANEL_INSET * 2 - TAB_GAP * 2) / LEFT_TAB_COUNT;
		for (int i = 0; i < TAB_NAMES.length; i++) {
			int index = i;
			int tabWidth = i < LEFT_TAB_COUNT ? leftTabWidth : (sideWidth - PANEL_INSET * 2 - TAB_GAP) / 2;
			int x = i < LEFT_TAB_COUNT
					? left + PANEL_INSET + i * (leftTabWidth + TAB_GAP)
					: right + PANEL_INSET + (i - LEFT_TAB_COUNT) * (tabWidth + TAB_GAP);
			Button button = addButton(x, top + TAB_Y_OFFSET, tabWidth, CONTROL_HEIGHT,
					TAB_NAMES[i], () -> {
						tab = index;
						rebuildWidgets();
					});
			button.active = i != tab;
		}
		addButton(right + sideWidth - CLOSE_BUTTON_INSET, top + CLOSE_BUTTON_Y, CLOSE_BUTTON_WIDTH, COMPACT_BUTTON_HEIGHT, "×", this::onClose);
	}

	private void buildStandTab() {
		int y = top + CHECKBOX_ROW_Y_OFFSET;
		int firstX = left + CHECKBOX_COLUMN_X_OFFSET;
		int secondX = right + CHECKBOX_COLUMN_X_OFFSET;
		addCheckbox(firstX, y, "Base plate", state.basePlate(), Action.BASE_PLATE);
		addCheckbox(secondX, y, "Show arms", state.arms(), Action.ARMS);
		addCheckbox(firstX, y + CHECKBOX_ROW_SPACING, "Small stand", state.small(), Action.SMALL);
		addCheckbox(secondX, y + CHECKBOX_ROW_SPACING, "Gravity", state.gravity(), Action.GRAVITY);
		addCheckbox(firstX, y + CHECKBOX_ROW_SPACING * 2, "Visible", state.visible(), Action.VISIBLE);
		addCheckbox(secondX, y + CHECKBOX_ROW_SPACING * 2, "Show name", state.nameVisible(), Action.NAME_VISIBLE);
		addCheckbox(firstX, y + CHECKBOX_ROW_SPACING * 3, "Owner locked", state.locked(), Action.LOCK);
		addCheckbox(secondX, y + CHECKBOX_ROW_SPACING * 3, "Invulnerable", state.invulnerable(), Action.INVULNERABLE);
	}

	private void buildPoseTab() {
		Button angles = addButton(left + POSE_MODE_BUTTON_X_OFFSET, top + SECONDARY_ROW_Y_OFFSET,
				POSE_MODE_BUTTON_WIDTH, COMPACT_BUTTON_HEIGHT, "Angles", () -> {
			pointingMode = false;
			rebuildWidgets();
		});
		Button pointing = addButton(left + POSE_MODE_BUTTON_X_OFFSET + POSE_MODE_BUTTON_WIDTH + POSE_MODE_BUTTON_GAP,
				top + SECONDARY_ROW_Y_OFFSET, POSE_MODE_BUTTON_WIDTH, COMPACT_BUTTON_HEIGHT, "Pointing", () -> {
			pointingMode = true;
			rebuildWidgets();
		});
		angles.active = pointingMode;
		pointing.active = !pointingMode;
		if (pointingMode) {
			buildPointingControls();
		} else {
			buildAngleControls();
		}
	}

	private void buildAngleControls() {
		int rowY = top + POSE_LABEL_Y_OFFSET;
		for (int part = 0; part < BodyPart.values().length; part++) {
			int partIndex = part;
			int sideX = part < PARTS_PER_PANEL ? left : right;
			int y = rowY + (part % PARTS_PER_PANEL) * POSE_ROW_SPACING + POSE_CONTROL_Y_OFFSET;
			int buttonWidth = (sideWidth - 24) / 7;
			for (int axis = 0; axis < 3; axis++) {
				int axisIndex = axis;
				String axisName = "XYZ".substring(axis, axis + 1);
				addButton(sideX + PANEL_INSET + axis * buttonWidth * 2, y, buttonWidth - 2, CONTROL_HEIGHT, "−" + axisName,
						() -> send(Action.ADJUST, partIndex, axisIndex, -angleStep));
				addButton(sideX + PANEL_INSET + axis * buttonWidth * 2 + buttonWidth, y, buttonWidth - 2, CONTROL_HEIGHT, "+" + axisName,
						() -> send(Action.ADJUST, partIndex, axisIndex, angleStep));
			}
			addButton(sideX + PANEL_INSET + buttonWidth * 6, y, buttonWidth, CONTROL_HEIGHT, "↺", () -> send(Action.RESET_PART, partIndex, 0, 0));
		}
		addButton(right + sideWidth - STEP_BUTTON_X_INSET, top + SECONDARY_ROW_Y_OFFSET, STEP_BUTTON_WIDTH, COMPACT_BUTTON_HEIGHT, "Step: " + degree(angleStep), () -> {
			angleStep = ArmorStandActions.nextAngleStep(angleStep);
			rebuildWidgets();
		});
	}

	private void buildPointingControls() {
		int rowY = top + POSE_LABEL_Y_OFFSET + 4;
		for (int part = 0; part < BodyPart.values().length; part++) {
			int partIndex = part;
			int sideX = part < PARTS_PER_PANEL ? left : right;
			int y = rowY + (part % PARTS_PER_PANEL) * POSE_ROW_SPACING + POSE_CONTROL_Y_OFFSET;
			int buttonWidth = (sideWidth - 22) / 2;
			addButton(sideX + PANEL_INSET, y, buttonWidth, CONTROL_HEIGHT, "Point at eyes", () -> send(Action.POINT, partIndex, 1, 0));
			addButton(sideX + 12 + buttonWidth, y, buttonWidth, CONTROL_HEIGHT, "Point at feet", () -> send(Action.POINT, partIndex, 0, 0));
		}
	}

	private void buildPositionTab() {
		double[] amounts = {-0.5, -0.1875, -0.0625, 0.0625, 0.1875, 0.5};
		String[] amountNames = {"−½", "−3/16", "−1/16", "+1/16", "+3/16", "+½"};
		for (int axis = 0; axis < 3; axis++) {
			int axisIndex = axis;
			int sideX = axis < AXES_ON_LEFT_PANEL ? left : right;
			int y = top + POSITION_ROW_Y_OFFSET + (axis % 2) * POSITION_ROW_SPACING;
			int buttonWidth = (sideWidth - 22) / 6;
			for (int i = 0; i < amounts.length; i++) {
				double amount = amounts[i];
				addButton(sideX + PANEL_INSET + i * buttonWidth, y, buttonWidth - 2, SECONDARY_BUTTON_HEIGHT, amountNames[i], () -> send(Action.MOVE, axisIndex, 0, amount));
			}
		}
		int yawY = top + YAW_ROW_Y_OFFSET;
		int yawWidth = (sideWidth - 22) / 4;
		addButton(right + PANEL_INSET, yawY, yawWidth, SECONDARY_BUTTON_HEIGHT, "−45°", () -> send(Action.ROTATE, 0, 0, -45));
		addButton(right + 10 + yawWidth, yawY, yawWidth, SECONDARY_BUTTON_HEIGHT, "−" + degree(angleStep), () -> send(Action.ROTATE, 0, 0, -angleStep));
		addButton(right + 12 + yawWidth * 2, yawY, yawWidth, SECONDARY_BUTTON_HEIGHT, "+" + degree(angleStep), () -> send(Action.ROTATE, 0, 0, angleStep));
		addButton(right + 14 + yawWidth * 3, yawY, yawWidth, SECONDARY_BUTTON_HEIGHT, "+45°", () -> send(Action.ROTATE, 0, 0, 45));
		addButton(right + PANEL_INSET, top + FACE_CENTER_Y_OFFSET, (sideWidth - 20) / 2, SECONDARY_BUTTON_HEIGHT, "Face me", () -> send(Action.FACE_PLAYER, 0, 0, 0));
		addButton(right + 12 + (sideWidth - 20) / 2, top + FACE_CENTER_Y_OFFSET, (sideWidth - 20) / 2, SECONDARY_BUTTON_HEIGHT, "Center", () -> send(Action.CENTER, 0, 0, 0));
		addButton(right + sideWidth - STEP_BUTTON_X_INSET, top + SECONDARY_ROW_Y_OFFSET, STEP_BUTTON_WIDTH, COMPACT_BUTTON_HEIGHT, "Step: " + degree(angleStep), () -> {
			angleStep = ArmorStandActions.nextAngleStep(angleStep);
			rebuildWidgets();
		});
	}

	private void buildPresetsTab() {
		int columns = 2;
		int gap = 5;
		int buttonWidth = (sideWidth - 24 - gap) / columns;
		for (int i = 0; i < ArmorStandActions.PRESETS.size(); i++) {
			int preset = i;
			int local = i % PRESETS_PER_PANEL;
			int sideX = i < PRESETS_PER_PANEL ? left : right;
			int x = sideX + 12 + (local % columns) * (buttonWidth + gap);
			int y = top + PRESET_ROW_Y_OFFSET + (local / columns) * PRESET_ROW_SPACING;
			addButton(x, y, buttonWidth, ACTION_BUTTON_HEIGHT, ArmorStandActions.PRESETS.get(i).name(), () -> send(Action.PRESET, preset, 0, 0));
		}
		addButton(right + sideWidth / 2 - RANDOM_POSE_WIDTH / 2, top + RANDOM_POSE_Y_OFFSET, RANDOM_POSE_WIDTH, SECONDARY_BUTTON_HEIGHT, "Random pose", () -> send(Action.RANDOM, 0, 0, 0));
	}

	private void buildToolsTab() {
		int x1 = left + TOOLS_BUTTON_X_INSET;
		int x2 = right + TOOLS_BUTTON_X_INSET;
		int w = sideWidth - TOOLS_BUTTON_WIDTH_MARGIN;
		int y = top + TOOLS_ROW_Y_OFFSET;
		addButton(x1, y, w, ACTION_BUTTON_HEIGHT, "Copy pose", () -> send(Action.COPY, 0, 0, 0));
		addButton(x2, y, w, ACTION_BUTTON_HEIGHT, "Paste pose", () -> send(Action.PASTE, 0, 0, 0));
		addButton(x1, y + TOOLS_ROW_SPACING, w, ACTION_BUTTON_HEIGHT, "Reset pose", () -> send(Action.RESET_POSE, 0, 0, 0));
		addButton(x2, y + TOOLS_ROW_SPACING, w, ACTION_BUTTON_HEIGHT, "Flip whole pose", () -> send(Action.FLIP, 0, 0, 0));
		addButton(x1, y + TOOLS_ROW_SPACING * 2, w, ACTION_BUTTON_HEIGHT, "Swap hands", () -> send(Action.SWAP_HANDS, 0, 0, 0));
		addButton(x2, y + TOOLS_ROW_SPACING * 2, w, ACTION_BUTTON_HEIGHT, "Swap hand / head", () -> send(Action.SWAP_HEAD, 0, 0, 0));
		addButton(x1, y + TOOLS_ROW_SPACING * 3, w, ACTION_BUTTON_HEIGHT, "Mirror left arm → right", () -> send(Action.MIRROR, 2, 3, 0));
		addButton(x2, y + TOOLS_ROW_SPACING * 3, w, ACTION_BUTTON_HEIGHT, "Mirror right arm → left", () -> send(Action.MIRROR, 3, 2, 0));
		addButton(x1, y + TOOLS_ROW_SPACING * 4, w, ACTION_BUTTON_HEIGHT, "Mirror left leg → right", () -> send(Action.MIRROR, 4, 5, 0));
		addButton(x2, y + TOOLS_ROW_SPACING * 4, w, ACTION_BUTTON_HEIGHT, "Mirror right leg → left", () -> send(Action.MIRROR, 5, 4, 0));
	}

	private void addCheckbox(int x, int y, String label, boolean checked, Action action) {
		addRenderableWidget(Checkbox.builder(Component.literal(label), font)
				.pos(x, y).selected(checked).maxWidth(sideWidth - 38)
				.onValueChange((checkbox, value) -> send(action, 0, 0, 0)).build());
	}

	private Button addButton(int x, int y, int width, int height, String label, Runnable action) {
		return addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
				.bounds(x, y, width, height).build());
	}

	private void send(Action action, int first, int second, double value) {
		ClientPlayNetworking.send(new ActionPayload(entityId, action, first, second, value));
	}

	private static String degree(double value) {
		return (int) value + "°";
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(left, top, left + sideWidth, top + panelHeight, 0xD8101010);
		graphics.fill(right, top, right + sideWidth, top + panelHeight, 0xD8101010);
		graphics.outline(left, top, sideWidth, panelHeight, 0xFF777777);
		graphics.outline(right, top, sideWidth, panelHeight, 0xFF777777);
		graphics.centeredText(font, title, left + sideWidth / 2, top + PANEL_MARGIN, 0xFFFFFFFF);
		if (tab == 1) {
			for (int part = 0; part < BodyPart.values().length; part++) {
				int sideX = part < PARTS_PER_PANEL ? left : right;
				int y = top + POSE_LABEL_Y_OFFSET + (part % PARTS_PER_PANEL) * POSE_ROW_SPACING;
				graphics.text(font, BodyPart.values()[part].label(), sideX + PANEL_MARGIN, y + 5, 0xFFE0E0E0);
			}
		}
		if (tab == 2) {
			graphics.text(font, "X position", left + PANEL_MARGIN, top + POSITION_LABEL_Y_OFFSET, 0xFFFFFFFF);
			graphics.text(font, "Y position", left + PANEL_MARGIN, top + POSITION_LABEL_Y_OFFSET + POSITION_ROW_SPACING, 0xFFFFFFFF);
			graphics.text(font, "Z position", right + PANEL_MARGIN, top + POSITION_LABEL_Y_OFFSET, 0xFFFFFFFF);
			graphics.text(font, "Yaw", right + PANEL_MARGIN, top + POSITION_LABEL_Y_OFFSET + POSITION_ROW_SPACING, 0xFFFFFFFF);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
