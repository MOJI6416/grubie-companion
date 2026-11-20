package com.grubielauncher.companion.mc;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.PanelModel;
import com.grubielauncher.companion.core.CompanionState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class PanelScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int ROW = 22;
    private static final int BUTTON = 70;

    private final Screen parent;
    private final List<PanelModel.Row> rows = new ArrayList<>();
    private int revision = -1;
    private int top;

    public PanelScreen(Screen parent) {
        super(Component.translatable("grubie_companion.panel.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Companion companion = Companion.get();
        rows.clear();
        if (companion != null) {
            CompanionState.View view = companion.state().view();
            revision = view.revision();
            rows.addAll(PanelModel.rows(view));
        }

        int height = rows.size() * ROW;
        top = Math.max(32, (this.height - height - 30) / 2);
        int left = (this.width - WIDTH) / 2;
        for (int index = 0; index < rows.size(); index++) {
            PanelModel.Row row = rows.get(index);
            if (row.action() == null) continue;
            int y = top + index * ROW;
            this.addRenderableWidget(Button.builder(PanelModel.actionLabel(row.action()), button -> {
                        PanelModel.run(row.action(), row.target());
                        button.active = false;
                        if (row.action() == PanelModel.Action.ACCEPT) onClose();
                    })
                    .bounds(left + WIDTH - BUTTON, y, BUTTON, 20)
                    .build());
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 75, top + height + 8, 150, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        Companion companion = Companion.get();
        if (companion != null && companion.state().revision() != revision) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 20, PanelModel.HEADING);
        int left = (this.width - WIDTH) / 2;
        for (int index = 0; index < rows.size(); index++) {
            PanelModel.Row row = rows.get(index);
            int y = top + index * ROW + 6;
            int maxWidth = row.action() == null ? WIDTH : WIDTH - BUTTON - 8;
            String text = this.font.plainSubstrByWidth(row.text().getString(), maxWidth);
            graphics.drawString(this.font, text, left + (row.heading() ? 0 : 6), y, row.color());
        }
    }

    @Override
    public void onClose() {
        Mc.setScreen(this.minecraft, parent);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null;
    }
}
