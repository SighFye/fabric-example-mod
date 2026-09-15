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
		sideWidth = Math.min(300, Math.max(150, (width - 200) / 2));
		left = 10;
		right = width - sideWidth - 10;
		top = 10;
		panelHeight = height - 20;
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
		int gap = 2;
		int leftTabWidth = (sideWidth - 16 - gap * 2) / 3;
		for (int i = 0; i < TAB_NAMES.length; i++) {
			int index = i;
			int tabWidth = i < 3 ? leftTabWidth : (sideWidth - 18) / 2;
			int x = i < 3 ? left + 8 + i * (leftTabWidth + gap) : right + 8 + (i - 3) * (tabWidth + gap);
			Button button = addButton(x, top + 28, tabWidth, 20,
					TAB_NAMES[i], () -> {
						tab = index;
						rebuildWidgets();
					});
			button.active = i != tab;
		}
		addButton(right + sideWidth - 28, top + 5, 20, 18, "×", this::onClose);
	}

	private void buildStandTab() {
		int y = top + 68;
		int firstX = left + 20;
		int secondX = right + 20;
		addCheckbox(firstX, y, "Base plate", state.basePlate(), Action.BASE_PLATE);
		addCheckbox(secondX, y, "Show arms", state.arms(), Action.ARMS);
		addCheckbox(firstX, y + 30, "Small stand", state.small(), Action.SMALL);
		addCheckbox(secondX, y + 30, "Gravity", state.gravity(), Action.GRAVITY);
		addCheckbox(firstX, y + 60, "Visible", state.visible(), Action.VISIBLE);
		addCheckbox(secondX, y + 60, "Show name", state.nameVisible(), Action.NAME_VISIBLE);
		addCheckbox(firstX, y + 90, "Owner locked", state.locked(), Action.LOCK);
		addCheckbox(secondX, y + 90, "Invulnerable", state.invulnerable(), Action.INVULNERABLE);
	}

	private void buildPoseTab() {
		Button angles = addButton(left + 10, top + 55, 82, 18, "Angles", () -> {
			pointingMode = false;
			rebuildWidgets();
		});
		Button pointing = addButton(left + 96, top + 55, 82, 18, "Pointing", () -> {
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
		int rowY = top + 78;
		for (int part = 0; part < BodyPart.values().length; part++) {
			int partIndex = part;
			int sideX = part < 3 ? left : right;
			int y = rowY + (part % 3) * 47 + 17;
			int buttonWidth = (sideWidth - 24) / 7;
			for (int axis = 0; axis < 3; axis++) {
				int axisIndex = axis;
				String axisName = "XYZ".substring(axis, axis + 1);
				addButton(sideX + 8 + axis * buttonWidth * 2, y, buttonWidth - 2, 20, "−" + axisName,
						() -> send(Action.ADJUST, partIndex, axisIndex, -angleStep));
				addButton(sideX + 8 + axis * buttonWidth * 2 + buttonWidth, y, buttonWidth - 2, 20, "+" + axisName,
						() -> send(Action.ADJUST, partIndex, axisIndex, angleStep));
			}
			addButton(sideX + 8 + buttonWidth * 6, y, buttonWidth, 20, "↺", () -> send(Action.RESET_PART, partIndex, 0, 0));
		}
		addButton(right + sideWidth - 105, top + 55, 95, 18, "Step: " + degree(angleStep), () -> {
			angleStep = ArmorStandActions.nextAngleStep(angleStep);
			rebuildWidgets();
		});
	}

	private void buildPointingControls() {
		int rowY = top + 82;
		for (int part = 0; part < BodyPart.values().length; part++) {
			int partIndex = part;
			int sideX = part < 3 ? left : right;
			int y = rowY + (part % 3) * 47 + 17;
			int buttonWidth = (sideWidth - 22) / 2;
			addButton(sideX + 8, y, buttonWidth, 20, "Point at eyes", () -> send(Action.POINT, partIndex, 1, 0));
			addButton(sideX + 12 + buttonWidth, y, buttonWidth, 20, "Point at feet", () -> send(Action.POINT, partIndex, 0, 0));
		}
	}

	private void buildPositionTab() {
		double[] amounts = {-0.5, -0.1875, -0.0625, 0.0625, 0.1875, 0.5};
		String[] amountNames = {"−½", "−3/16", "−1/16", "+1/16", "+3/16", "+½"};
		for (int axis = 0; axis < 3; axis++) {
			int axisIndex = axis;
			int sideX = axis < 2 ? left : right;
			int y = top + 83 + (axis % 2) * 54;
			int buttonWidth = (sideWidth - 22) / 6;
			for (int i = 0; i < amounts.length; i++) {
				double amount = amounts[i];
				addButton(sideX + 8 + i * buttonWidth, y, buttonWidth - 2, 22, amountNames[i], () -> send(Action.MOVE, axisIndex, 0, amount));
			}
		}
		int yawY = top + 137;
		int yawWidth = (sideWidth - 22) / 4;
		addButton(right + 8, yawY, yawWidth, 22, "−45°", () -> send(Action.ROTATE, 0, 0, -45));
		addButton(right + 10 + yawWidth, yawY, yawWidth, 22, "−" + degree(angleStep), () -> send(Action.ROTATE, 0, 0, -angleStep));
		addButton(right + 12 + yawWidth * 2, yawY, yawWidth, 22, "+" + degree(angleStep), () -> send(Action.ROTATE, 0, 0, angleStep));
		addButton(right + 14 + yawWidth * 3, yawY, yawWidth, 22, "+45°", () -> send(Action.ROTATE, 0, 0, 45));
		addButton(right + 8, top + 170, (sideWidth - 20) / 2, 22, "Face me", () -> send(Action.FACE_PLAYER, 0, 0, 0));
		addButton(right + 12 + (sideWidth - 20) / 2, top + 170, (sideWidth - 20) / 2, 22, "Center", () -> send(Action.CENTER, 0, 0, 0));
		addButton(right + sideWidth - 105, top + 55, 95, 18, "Step: " + degree(angleStep), () -> {
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
			int local = i % 10;
			int sideX = i < 10 ? left : right;
			int x = sideX + 12 + (local % columns) * (buttonWidth + gap);
			int y = top + 62 + (local / columns) * 30;
			addButton(x, y, buttonWidth, 24, ArmorStandActions.PRESETS.get(i).name(), () -> send(Action.PRESET, preset, 0, 0));
		}
		addButton(right + sideWidth / 2 - 58, top + 207, 116, 22, "Random pose", () -> send(Action.RANDOM, 0, 0, 0));
	}

	private void buildToolsTab() {
		int x1 = left + 18;
		int x2 = right + 18;
		int w = sideWidth - 36;
		int y = top + 69;
		addButton(x1, y, w, 24, "Copy pose", () -> send(Action.COPY, 0, 0, 0));
		addButton(x2, y, w, 24, "Paste pose", () -> send(Action.PASTE, 0, 0, 0));
		addButton(x1, y + 32, w, 24, "Reset pose", () -> send(Action.RESET_POSE, 0, 0, 0));
		addButton(x2, y + 32, w, 24, "Flip whole pose", () -> send(Action.FLIP, 0, 0, 0));
		addButton(x1, y + 64, w, 24, "Swap hands", () -> send(Action.SWAP_HANDS, 0, 0, 0));
		addButton(x2, y + 64, w, 24, "Swap hand / head", () -> send(Action.SWAP_HEAD, 0, 0, 0));
		addButton(x1, y + 96, w, 24, "Mirror left arm → right", () -> send(Action.MIRROR, 2, 3, 0));
		addButton(x2, y + 96, w, 24, "Mirror right arm → left", () -> send(Action.MIRROR, 3, 2, 0));
		addButton(x1, y + 128, w, 24, "Mirror left leg → right", () -> send(Action.MIRROR, 4, 5, 0));
		addButton(x2, y + 128, w, 24, "Mirror right leg → left", () -> send(Action.MIRROR, 5, 4, 0));
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
		graphics.centeredText(font, title, left + sideWidth / 2, top + 10, 0xFFFFFFFF);
		if (tab == 1) {
			for (int part = 0; part < BodyPart.values().length; part++) {
				int sideX = part < 3 ? left : right;
				int y = top + 78 + (part % 3) * 47;
				graphics.text(font, BodyPart.values()[part].label(), sideX + 10, y + 5, 0xFFE0E0E0);
			}
		}
		if (tab == 2) {
			graphics.text(font, "X position", left + 10, top + 70, 0xFFFFFFFF);
			graphics.text(font, "Y position", left + 10, top + 124, 0xFFFFFFFF);
			graphics.text(font, "Z position", right + 10, top + 70, 0xFFFFFFFF);
			graphics.text(font, "Yaw", right + 10, top + 124, 0xFFFFFFFF);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
