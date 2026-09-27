package dev.belandsigh.client;

import dev.belandsigh.config.Feature;
import dev.belandsigh.config.SettingsNetwork.RequestPayload;
import dev.belandsigh.config.SettingsNetwork.StatePayload;
import dev.belandsigh.config.SettingsNetwork.UpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class BelAndSighSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 540;
	private static final int PANEL_HEIGHT = 310;
	private static final int SIDEBAR_WIDTH = 184;
	private static final int TAB_HEIGHT = 20;
	private static final int TABS_PER_PAGE = 9;
	private static BelAndSighSettingsScreen current;

	private final Screen parent;
	private StatePayload state;
	private Feature selected = Feature.MORE_MOB_HEADS;
	private int page = selected.ordinal() / TABS_PER_PAGE;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;

	public BelAndSighSettingsScreen(Screen parent) {
		super(Component.literal("BelAndSigh Settings"));
		this.parent = parent;
		this.state = SettingsClientState.state();
		current = this;
	}

	public static void updateCurrent(StatePayload state) {
		if (current != null) {
			current.state = state;
			current.rebuildWidgets();
		}
	}

	@Override
	protected void init() {
		panelWidth = Math.min(PANEL_WIDTH, width - 20);
		panelHeight = Math.min(PANEL_HEIGHT, height - 20);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;

		addButton(left + panelWidth - 62, top + 8, 52, 20, "Done", this::onClose);
		buildTabs();
		if (state == null) {
			ClientPlayNetworking.send(new RequestPayload());
			return;
		}

		int contentLeft = left + SIDEBAR_WIDTH + 20;
		int contentWidth = panelWidth - SIDEBAR_WIDTH - 34;
		Button toggle = addButton(contentLeft, top + 102, contentWidth, 24,
			state.enabled(selected) ? "Enabled" : "Disabled", () ->
				ClientPlayNetworking.send(UpdatePayload.feature(selected, !state.enabled(selected))));
		toggle.active = state.editable();

		Button disableAll = addButton(contentLeft, top + panelHeight - 58,
			(contentWidth - 6) / 2, 20, "Disable all", () -> ClientPlayNetworking.send(UpdatePayload.all(false)));
		Button defaults = addButton(contentLeft + (contentWidth + 6) / 2, top + panelHeight - 58,
			(contentWidth - 6) / 2, 20, "Restore defaults", () -> ClientPlayNetworking.send(UpdatePayload.defaults()));
		disableAll.active = state.editable();
		defaults.active = state.editable();
	}

	private void buildTabs() {
		Feature[] features = Feature.values();
		int start = page * TABS_PER_PAGE;
		int end = Math.min(features.length, start + TABS_PER_PAGE);
		for (int i = start; i < end; i++) {
			Feature feature = features[i];
			Button tab = addButton(left + 10, top + 38 + (i - start) * (TAB_HEIGHT + 3),
				SIDEBAR_WIDTH - 20, TAB_HEIGHT, feature.title(), () -> {
					selected = feature;
					rebuildWidgets();
				});
			tab.active = feature != selected;
		}
		int navY = top + panelHeight - 30;
		Button previous = addButton(left + 10, navY, 78, 20, "Previous", () -> {
			page--;
			rebuildWidgets();
		});
		Button next = addButton(left + 96, navY, 78, 20, "Next", () -> {
			page++;
			rebuildWidgets();
		});
		previous.active = page > 0;
		next.active = (page + 1) * TABS_PER_PAGE < features.length;
	}

	private Button addButton(int x, int y, int buttonWidth, int buttonHeight, String label, Runnable action) {
		return addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
			.bounds(x, y, buttonWidth, buttonHeight).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0101010);
		graphics.outline(left, top, panelWidth, panelHeight, 0xFF777777);
		graphics.fill(left + SIDEBAR_WIDTH, top + 30, left + SIDEBAR_WIDTH + 1, top + panelHeight - 10, 0xFF555555);
		graphics.text(font, title, left + 12, top + 14, 0xFFFFFFFF);

		int contentLeft = left + SIDEBAR_WIDTH + 20;
		graphics.text(font, selected.title(), contentLeft, top + 44, 0xFFFFFFFF);
		int descriptionY = top + 64;
		for (FormattedCharSequence line : font.split(Component.literal(selected.description()),
				panelWidth - SIDEBAR_WIDTH - 40)) {
			graphics.text(font, line, contentLeft, descriptionY, 0xFFB8B8B8);
			descriptionY += 11;
		}
		if (state == null) {
			graphics.text(font, "Loading server settings...", contentLeft, top + 104, 0xFFAAAAAA);
		} else {
			graphics.text(font, state.editable() ? "Server setting" : "Server setting (operator access required)",
				contentLeft, top + 88, state.editable() ? 0xFFAAAAAA : 0xFFFFAA55);
			if (selected == Feature.MORE_MOB_HEADS || selected == Feature.MINI_BLOCKS) {
				graphics.text(font, "Run /reload or restart after changing this feature.",
					contentLeft, top + 140, 0xFFFFCC66);
			}
			graphics.text(font, "Settings are saved for this server.",
				contentLeft, top + panelHeight - 82, 0xFF888888);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
}
