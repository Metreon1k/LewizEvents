///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy
package dev.lewiz.events.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lewiz.events.logic.EventTracker;
import dev.lewiz.events.model.EventInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ApiClient {

    private static final URI URL = URI.create("https://api.holyworld.me/v1/events");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);

    private ApiClient() {
    }

    public static void refresh() {
        if (!BUSY.compareAndSet(false, true)) return;
        try {
            HttpRequest req = HttpRequest.newBuilder(URL)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "LewizEvents/1.0")
                    .GET()
                    .build();

            HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .whenComplete((resp, err) -> {
                        try {
                            if (err != null) {
                                Throwable root = err.getCause() != null ? err.getCause() : err;
                                throw new IOException(String.valueOf(root.getMessage()));
                            }
                            if (resp.statusCode() != 200) {
                                throw new IOException("HTTP " + resp.statusCode());
                            }
                            EventTracker.get().update(parse(resp.body()));
                        } catch (Exception e) {
                            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                            EventTracker.get().fail(msg);
                        } finally {
                            BUSY.set(false);
                        }
                    });
        } catch (Exception e) {
            EventTracker.get().fail(String.valueOf(e.getMessage()));
            BUSY.set(false);
        }
    }

    static List<EventInfo> parse(String body) {
        List<EventInfo> result = new ArrayList<>();
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) return result;

        for (Map.Entry<String, JsonElement> server : root.getAsJsonObject().entrySet()) {
            if (!server.getValue().isJsonArray()) continue;
            JsonArray arr = server.getValue().getAsJsonArray();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();

                String id = str(o, "id", "UNKNOWN");
                String instanceId = str(o, "instanceId", server.getKey() + ":" + id);
                String name = id;
                String rare = "";
                if (o.has("metadata") && o.get("metadata").isJsonObject()) {
                    JsonObject meta = o.getAsJsonObject("metadata");
                    name = str(meta, "displayName", id);
                    rare = str(meta, "rare", "");
                }
                result.add(new EventInfo(server.getKey(), instanceId, id, name, rare));
            }
        }
        return result;
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        try {
            return e.getAsString();
        } catch (Exception ex) {
            return def;
        }
    }
}
