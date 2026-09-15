package dev.belandsigh.client;

import dev.belandsigh.mounts.MountCategory;
import dev.belandsigh.mounts.MountNetworking.ManagementDataPayload;
import dev.belandsigh.mounts.MountNetworking.MountSummary;
import dev.belandsigh.mounts.MountNetworking.RequestManagementDataPayload;
import dev.belandsigh.mounts.MountNetworking.SelectionStatePayload;
import dev.belandsigh.mounts.MountNetworking.SetSelectionPayload;
import dev.belandsigh.mounts.MountNetworking.UnbindMountPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MountManagementScreen extends Screen {
	private static final int PANEL_WIDTH = 440;
	private static final int PANEL_HEIGHT = 276;
	private static final int ROW_HEIGHT = 34;
	private static final int PAGE_SIZE = 4;
	private static final int MAIN_ROW_SPACING = 48;
	private static final int MAIN_CARD_HEIGHT = 42;
	private static MountManagementScreen current;

	private final Screen parent;
	private SelectionStatePayload selections = new SelectionStatePayload(
		Optional.empty(), Optional.empty(), Optional.empty());
	private List<MountSummary> mounts = List.of();
	private View view = View.MAIN;
	private MountCategory choosingCategory = MountCategory.LAND;
	private int page;
	private boolean requested;
	private boolean loading = true;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;

	public MountManagementScreen(Screen parent) {
		super(Component.translatable("screen.belandsigh.mount_management"));
		this.parent = parent;
		current = this;
	}

	public static void updateManagementData(ManagementDataPayload payload) {
		if (current == null) {
			return;
		}
		current.selections = payload.selections();
		current.mounts = payload.mounts();
		current.loading = false;
		current.clampPage();
		current.rebuildWidgets();
	}

	public static void updateSelections(SelectionStatePayload selections) {
		if (current == null) {
			return;
		}
		current.selections = selections;
		current.rebuildWidgets();
	}

	@Override
	protected void init() {
		panelWidth = Math.min(PANEL_WIDTH, width - 20);
		panelHeight = Math.min(PANEL_HEIGHT, height - 20);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;

		addButton(left + panelWidth - 58, top + 8, 48, 20,
			Component.translatable("screen.belandsigh.mount_management.close"), this::onClose);
		if (!requested) {
			requested = true;
			ClientPlayNetworking.send(new RequestManagementDataPayload());
		}
		if (loading) {
			return;
		}

		switch (view) {
			case MAIN -> buildMainView();
			case CHOOSE -> buildChooseView();
			case BOUND -> buildBoundView();
		}
	}

	private void buildMainView() {
		for (int i = 0; i < MountCategory.values().length; i++) {
			MountCategory category = MountCategory.values()[i];
			int y = top + 40 + i * MAIN_ROW_SPACING;
			addButton(left + panelWidth - 150, y + 15, 68, 20,
				Component.translatable("screen.belandsigh.mount_management.choose"), () -> {
					choosingCategory = category;
					view = View.CHOOSE;
					page = 0;
					rebuildWidgets();
				});
			addButton(left + panelWidth - 76, y + 15, 58, 20,
				Component.translatable("screen.belandsigh.mount_management.clear"), () -> {
					ClientPlayNetworking.send(new SetSelectionPayload(category, Optional.empty()));
				});
		}
		addButton(left + 16, top + panelHeight - 30, 120, 20,
			Component.translatable("screen.belandsigh.mount_management.bound"), () -> {
				view = View.BOUND;
				page = 0;
				rebuildWidgets();
			});
	}

	private void buildChooseView() {
		addBackButton();
		List<MountSummary> compatible = mounts.stream()
			.filter(mount -> mount.supports(choosingCategory)).toList();
		buildPagedRows(compatible, false);
	}

	private void buildBoundView() {
		addBackButton();
		buildPagedRows(mounts.stream().filter(MountSummary::bound).toList(), true);
	}

	private void buildPagedRows(List<MountSummary> entries, boolean unbind) {
		int start = page * PAGE_SIZE;
		int end = Math.min(entries.size(), start + PAGE_SIZE);
		for (int i = start; i < end; i++) {
			MountSummary mount = entries.get(i);
			int y = top + 52 + (i - start) * ROW_HEIGHT;
			Component action = Component.translatable(unbind
				? "screen.belandsigh.mount_management.unbind"
				: "screen.belandsigh.mount_management.select");
			addButton(left + panelWidth - 84, y, 66, 20, action, () -> {
				if (unbind) {
					ClientPlayNetworking.send(new UnbindMountPayload(mount.mountUuid()));
				} else {
					ClientPlayNetworking.send(new SetSelectionPayload(
						choosingCategory, Optional.of(mount.mountUuid())));
					view = View.MAIN;
					rebuildWidgets();
				}
			});
		}

		int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		Button previous = addButton(left + panelWidth / 2 - 108, top + panelHeight - 30, 96, 20,
			Component.translatable("screen.belandsigh.mount_management.previous"), () -> {
				page--;
				rebuildWidgets();
			});
		Button next = addButton(left + panelWidth / 2 + 12, top + panelHeight - 30, 96, 20,
			Component.translatable("screen.belandsigh.mount_management.next"), () -> {
				page++;
				rebuildWidgets();
			});
		previous.active = page > 0;
		next.active = page + 1 < pages;
	}

	private void addBackButton() {
		addButton(left + 10, top + 8, 58, 20,
			Component.translatable("screen.belandsigh.mount_management.back"), () -> {
				view = View.MAIN;
				page = 0;
				rebuildWidgets();
			});
	}

	private Button addButton(int x, int y, int buttonWidth, int buttonHeight,
			Component label, Runnable action) {
		return addRenderableWidget(Button.builder(label, button -> action.run())
			.bounds(x, y, buttonWidth, buttonHeight).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xEE101010);
		graphics.outline(left, top, panelWidth, panelHeight, 0xFF777777);
		graphics.centeredText(font, title, left + panelWidth / 2, top + 14, 0xFFFFFFFF);

		if (loading) {
			graphics.centeredText(font,
				Component.translatable("screen.belandsigh.mount_management.loading"),
				left + panelWidth / 2, top + panelHeight / 2, 0xFFB0B0B0);
		} else if (view == View.MAIN) {
			renderMainView(graphics);
		} else {
			renderListView(graphics);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void renderMainView(GuiGraphicsExtractor graphics) {
		for (int i = 0; i < MountCategory.values().length; i++) {
			MountCategory category = MountCategory.values()[i];
			int y = top + 40 + i * MAIN_ROW_SPACING;
			graphics.fill(left + 12, y, left + panelWidth - 12, y + MAIN_CARD_HEIGHT, 0xCC242424);
			graphics.outline(left + 12, y, panelWidth - 24, MAIN_CARD_HEIGHT, 0xFF555555);
			graphics.text(font, categoryLabel(category), left + 20, y + 8, categoryColor(category));
			graphics.text(font, selectedMountLabel(category), left + 20, y + 26, 0xFFC8C8C8);
		}
	}

	private void renderListView(GuiGraphicsExtractor graphics) {
		List<MountSummary> entries = view == View.BOUND
			? mounts.stream().filter(MountSummary::bound).toList()
			: mounts.stream().filter(mount -> mount.supports(choosingCategory)).toList();
		Component heading = view == View.BOUND
			? Component.translatable("screen.belandsigh.mount_management.bound")
			: Component.literal(categoryLabel(choosingCategory).getString() + " Mounts");
		graphics.centeredText(font, heading, left + panelWidth / 2, top + 36, 0xFFFFFFFF);
		if (entries.isEmpty()) {
			graphics.centeredText(font,
				Component.translatable("screen.belandsigh.mount_management.empty"),
				left + panelWidth / 2, top + 90, 0xFFAAAAAA);
			return;
		}

		int start = page * PAGE_SIZE;
		int end = Math.min(entries.size(), start + PAGE_SIZE);
		for (int i = start; i < end; i++) {
			MountSummary mount = entries.get(i);
			int y = top + 52 + (i - start) * ROW_HEIGHT;
			graphics.text(font, mount.displayName(), left + 18, y + 2, 0xFFFFFFFF);
			graphics.text(font, mount.typeName() + " · " + mount.dimensionName(),
				left + 18, y + 14, 0xFF9A9A9A);
		}
	}

	private Component selectedMountLabel(MountCategory category) {
		Optional<UUID> selected = selectedUuid(category);
		if (selected.isEmpty()) {
			return Component.translatable("screen.belandsigh.mount_management.none");
		}
		return mounts.stream().filter(mount -> mount.mountUuid().equals(selected.get())).findFirst()
			.<Component>map(mount -> Component.literal(mount.displayName() + " · " + mount.typeName()))
			.orElseGet(() -> Component.literal(selected.get().toString().substring(0, 8) + "…"));
	}

	private Optional<UUID> selectedUuid(MountCategory category) {
		return switch (category) {
			case LAND -> selections.landMountUuid();
			case WATER -> selections.waterMountUuid();
			case LAVA -> selections.lavaMountUuid();
		};
	}

	private void clampPage() {
		int count = switch (view) {
			case MAIN -> 0;
			case CHOOSE -> (int) mounts.stream().filter(mount -> mount.supports(choosingCategory)).count();
			case BOUND -> (int) mounts.stream().filter(MountSummary::bound).count();
		};
		int maximumPage = Math.max(0, (count - 1) / PAGE_SIZE);
		page = Math.max(0, Math.min(page, maximumPage));
	}

	private static Component categoryLabel(MountCategory category) {
		String lower = category.name().toLowerCase(Locale.ROOT);
		return Component.literal(Character.toUpperCase(lower.charAt(0)) + lower.substring(1) + " Mount");
	}

	private static int categoryColor(MountCategory category) {
		return switch (category) {
			case LAND -> 0xFF80C060;
			case WATER -> 0xFF55AAFF;
			case LAVA -> 0xFFFF8050;
		};
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public void removed() {
		if (current == this) {
			current = null;
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private enum View {
		MAIN,
		CHOOSE,
		BOUND
	}
}
