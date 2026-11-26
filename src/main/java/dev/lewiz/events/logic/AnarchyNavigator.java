///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy

package dev.lewiz.events.logic;

import dev.lewiz.events.LewizEventsClient;
import dev.lewiz.events.model.AnarchyInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnarchyNavigator {

    private enum State { IDLE, HUB_WAIT, MAIN_MENU, MODE_MENU, ANARCHY_LIST }

    private static final int LITE_SLOT = 11;
    private static final int HUB_WAIT_AFTER_JOIN = 8;
    private static final int HUB_WAIT_NO_JOIN = 50;
    private static final int COMPASS_RETRY = 8;
    private static final int STABLE_TICKS = 2;
    private static final int TOTAL_TIMEOUT = 600;

    private static final AnarchyNavigator INSTANCE = new AnarchyNavigator();

    private State state = State.IDLE;
    private AnarchyInfo target;
    private int stateTicks;
    private int totalTicks;
    private int joinMark;
    private int lastCandidates;
    private int stableTicks;
    private int lastSyncId;
    private String lastTitle = "";

    public static AnarchyNavigator get() {
        return INSTANCE;
    }

    public boolean isBusy() {
        return state != State.IDLE;
    }

    public void start(AnarchyInfo anarchy) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (isBusy() || !anarchy.canJoin() || c.player == null) return;

        target = anarchy;
        totalTicks = 0;
        joinMark = -1;
        enter(State.HUB_WAIT);

        c.player.networkHandler.sendChatCommand("hub");
    }

    public void onJoin() {
        if (state == State.HUB_WAIT && joinMark < 0) joinMark = stateTicks;
    }

    public void tick(MinecraftClient c) {
        if (state == State.IDLE) return;
        stateTicks++;
        totalTicks++;

        if (totalTicks > TOTAL_TIMEOUT) {
            fail(c, "время ожидания вышло");
            return;
        }
        if (c.player == null || c.interactionManager == null) return;

        switch (state) {
            case HUB_WAIT -> tickHub(c);
            case MAIN_MENU -> tickMainMenu(c);
            case MODE_MENU -> tickModeMenu(c);
            case ANARCHY_LIST -> tickAnarchyList(c);
            default -> {
            }
        }
    }

    private void tickHub(MinecraftClient c) {
        boolean ready = joinMark >= 0 ? stateTicks - joinMark >= HUB_WAIT_AFTER_JOIN : stateTicks >= HUB_WAIT_NO_JOIN;
        if (ready) enter(State.MAIN_MENU);
    }

    private void tickMainMenu(MinecraftClient c) {
        HandledScreen<?> s = container(c);
        if (s != null) {
            Slot lite = slotById(s, c.player, LITE_SLOT);
            if (lite != null && !lite.getStack().isEmpty()) {
                remember(s);
                click(c, s, lite);
                enter(State.MODE_MENU);
            }
            return;
        }

        if (stateTicks % COMPASS_RETRY == 1) {
            int slot = findCompass(c.player);
            if (slot >= 0) useCompass(c, slot);
        }
    }

    private void tickModeMenu(MinecraftClient c) {
        HandledScreen<?> s = container(c);
        if (s == null || !isNew(s)) return;

        AnarchyInfo.Mode mode = target.mode;
        Slot slot = findByName(s, c.player, mode.label.toLowerCase(Locale.ROOT));
        if (slot == null) slot = findModeByCount(s, c.player, mode.teamSize);
        if (slot == null) return;

        remember(s);
        click(c, s, slot);
        enter(State.ANARCHY_LIST);
    }

    private void tickAnarchyList(MinecraftClient c) {
        HandledScreen<?> s = container(c);
        if (s == null) return;

        List<Slot> heads = new ArrayList<>();
        for (Slot sl : containerSlots(s, c.player)) {
            if (sl.id >= 9 && !sl.getStack().isEmpty()) heads.add(sl);
        }
        if (heads.isEmpty()) return;

        if (heads.size() != lastCandidates) {
            lastCandidates = heads.size();
            stableTicks = 0;
            return;
        }
        if (++stableTicks < STABLE_TICKS) return;

        int min = Integer.MAX_VALUE;
        StringBuilder dump = new StringBuilder();
        for (Slot sl : heads) {
            min = Math.min(min, sl.getStack().getCount());
            dump.append(sl.getStack().getCount()).append(' ');
        }
        int offset = (min == 1) ? target.mode.first - 1 : 0;
        int wanted = target.number - offset;

        Slot found = null;
        for (Slot sl : heads) {
            if (sl.getStack().getCount() == wanted) {
                found = sl;
                break;
            }
        }
        if (found == null) {
            LewizEventsClient.LOGGER.warn("Анархия {} не найдена, числа в меню: {}", target.number, dump);
            fail(c, "анархия " + target.number + " не найдена в меню");
            return;
        }
        click(c, s, found);
        state = State.IDLE;
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
        lastCandidates = -1;
        stableTicks = 0;
    }

    private void remember(HandledScreen<?> s) {
        lastSyncId = s.getScreenHandler().syncId;
        lastTitle = s.getTitle().getString();
    }

    private boolean isNew(HandledScreen<?> s) {
        return s.getScreenHandler().syncId != lastSyncId || !s.getTitle().getString().equals(lastTitle);
    }

    private void fail(MinecraftClient c, String reason) {
        state = State.IDLE;
        if (c.player != null) {
            c.player.sendMessage(Text.literal("[LewizEvents] Не получилось: " + reason).formatted(Formatting.RED), false);
        }
    }

    private static int findCompass(PlayerEntity p) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getStack(i).isOf(Items.COMPASS)) return i;
        }
        return -1;
    }

    private void useCompass(MinecraftClient c, int slot) {
        c.player.getInventory().selectedSlot = slot;
        c.interactionManager.interactItem(c.player, Hand.MAIN_HAND);
    }

    private static HandledScreen<?> container(MinecraftClient c) {
        if (c.currentScreen instanceof HandledScreen<?> hs && hs.getScreenHandler() instanceof GenericContainerScreenHandler) {
            return hs;
        }
        return null;
    }

    private static List<Slot> containerSlots(HandledScreen<?> s, PlayerEntity p) {
        List<Slot> list = new ArrayList<>();
        for (Slot sl : s.getScreenHandler().slots) {
            if (sl.inventory != p.getInventory()) list.add(sl);
        }
        return list;
    }

    private static Slot slotById(HandledScreen<?> s, PlayerEntity p, int id) {
        for (Slot sl : containerSlots(s, p)) {
            if (sl.id == id) return sl;
        }
        return null;
    }

    private static Slot findByName(HandledScreen<?> s, PlayerEntity p, String part) {
        for (Slot sl : containerSlots(s, p)) {
            ItemStack st = sl.getStack();
            if (st.isEmpty()) continue;
            if (st.getName().getString().toLowerCase(Locale.ROOT).contains(part)) return sl;
        }
        return null;
    }

    private static Slot findModeByCount(HandledScreen<?> s, PlayerEntity p, int teamSize) {
        for (Slot sl : containerSlots(s, p)) {
            if (sl.id <= 3 && !sl.getStack().isEmpty() && sl.getStack().getCount() == teamSize) return sl;
        }
        return null;
    }

    private static void click(MinecraftClient c, HandledScreen<?> s, Slot slot) {
        c.interactionManager.clickSlot(s.getScreenHandler().syncId, slot.id, 0, SlotActionType.PICKUP, c.player);
    }
}