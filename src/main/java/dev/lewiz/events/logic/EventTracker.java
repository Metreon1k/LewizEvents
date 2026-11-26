///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy
package dev.lewiz.events.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lewiz.events.LewizEventsClient;
import dev.lewiz.events.model.AnarchyInfo;
import dev.lewiz.events.model.EventInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventTracker {

    public record Entry(EventInfo info, long start, long end) {
    }

    private static final EventTracker INSTANCE = new EventTracker();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_HISTORY = 300;
    private static final long ACTIVE_KEEP_MS = 10 * 60 * 1000L;

    private final Map<String, Entry> active = new LinkedHashMap<>();
    private final Map<String, Long> missingSince = new HashMap<>();
    private final List<Entry> history = new ArrayList<>();
    private long lastUpdate;
    private String lastError;

    public static EventTracker get() {
        return INSTANCE;
    }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("lewizevents-data.json");
    }

    public synchronized void update(List<EventInfo> current) {
        long now = System.currentTimeMillis();
        Set<String> seen = new HashSet<>();

        for (EventInfo e : current) {
            seen.add(e.instanceId());
            Entry old = active.get(e.instanceId());
            active.put(e.instanceId(), new Entry(e, old == null ? now : old.start(), 0));
            missingSince.remove(e.instanceId());
        }

        Iterator<Map.Entry<String, Entry>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> me = it.next();
            if (seen.contains(me.getKey())) continue;
            long since = missingSince.computeIfAbsent(me.getKey(), k -> now);
            if (since != now) {
                Entry old = me.getValue();
                history.add(0, new Entry(old.info(), old.start(), since));
                missingSince.remove(me.getKey());
                it.remove();
            }
        }

        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);

        lastUpdate = now;
        lastError = null;
        save();
    }

    public synchronized void fail(String message) {
        lastError = message;
    }

    public synchronized List<Entry> active() {
        List<Entry> list = new ArrayList<>(active.values());
        list.sort(Comparator
                .comparingInt((Entry e) -> AnarchyInfo.parse(e.info().server()).sortKey())
                .thenComparing(e -> e.info().name()));
        return list;
    }

    public synchronized List<Entry> history() {
        return new ArrayList<>(history);
    }

    public synchronized long lastUpdate() {
        return lastUpdate;
    }

    public synchronized String lastError() {
        return lastError;
    }

    private static JsonObject toJson(Entry e) {
        JsonObject o = new JsonObject();
        o.addProperty("server", e.info().server());
        o.addProperty("instanceId", e.info().instanceId());
        o.addProperty("id", e.info().id());
        o.addProperty("name", e.info().name());
        o.addProperty("rare", e.info().rare());
        o.addProperty("start", e.start());
        o.addProperty("end", e.end());
        return o;
    }

    private static Entry fromJson(JsonObject o) {
        EventInfo info = new EventInfo(
                o.get("server").getAsString(),
                o.get("instanceId").getAsString(),
                o.get("id").getAsString(),
                o.get("name").getAsString(),
                o.get("rare").getAsString());
        return new Entry(info, o.get("start").getAsLong(), o.get("end").getAsLong());
    }

    private void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("savedAt", System.currentTimeMillis());
            JsonArray a = new JsonArray();
            active.values().forEach(e -> a.add(toJson(e)));
            JsonArray h = new JsonArray();
            history.forEach(e -> h.add(toJson(e)));
            root.add("active", a);
            root.add("history", h);
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LewizEventsClient.LOGGER.warn("Не удалось сохранить историю", e);
        }
    }

    public synchronized void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            long savedAt = root.get("savedAt").getAsLong();

            for (JsonElement el : root.getAsJsonArray("history")) {
                history.add(fromJson(el.getAsJsonObject()));
            }

            if (System.currentTimeMillis() - savedAt < ACTIVE_KEEP_MS) {
                for (JsonElement el : root.getAsJsonArray("active")) {
                    Entry e = fromJson(el.getAsJsonObject());
                    active.put(e.info().instanceId(), e);
                }
            }
        } catch (Exception e) {
            LewizEventsClient.LOGGER.warn("Не удалось прочитать историю, начинаю с чистой", e);
            active.clear();
            history.clear();
        }
    }
}
