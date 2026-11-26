///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy

package dev.lewiz.events;

import dev.lewiz.events.gui.EventsScreen;
import dev.lewiz.events.logic.AnarchyNavigator;
import dev.lewiz.events.logic.EventTracker;
import dev.lewiz.events.net.ApiClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LewizEventsClient implements ClientModInitializer {

    public static final String MOD_ID = "lewizevents";
    public static final Logger LOGGER = LoggerFactory.getLogger("LewizEvents");

    private static final int POLL_INTERVAL = 20;

    private static KeyBinding openKey;
    private int ticks = POLL_INTERVAL;

    public static KeyBinding openKey() {
        return openKey;
    }

    @Override
    public void onInitializeClient() {
        EventTracker.get().load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lewizevents.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.lewizevents"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticks = POLL_INTERVAL;
            AnarchyNavigator.get().onJoin();
        });
    }

    private void onTick(MinecraftClient client) {
        while (openKey.wasPressed()) {
            if (client.currentScreen instanceof EventsScreen) {
                client.setScreen(null);
            } else if (client.currentScreen == null && client.player != null) {
                client.setScreen(new EventsScreen());
            }
        }

        AnarchyNavigator.get().tick(client);

        if (client.world == null) return;
        ticks++;
        if (ticks >= POLL_INTERVAL) {
            ticks = 0;
            ApiClient.refresh();
        }
    }
}