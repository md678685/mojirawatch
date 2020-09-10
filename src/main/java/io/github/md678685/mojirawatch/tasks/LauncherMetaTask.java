package io.github.md678685.mojirawatch.tasks;

import io.github.md678685.mojirawatch.Config;
import io.github.md678685.mojirawatch.notifiers.Notifier;
import io.github.md678685.mojirawatch.util.DurationUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LauncherMetaTask implements Task {

    private final Config.LauncherMeta config;
    private final ScheduledExecutorService scheduler;
    private final OkHttpClient client;
    private final List<Notifier> notifiers;

    private final List<String> knownVersions = new ArrayList<>();

    public LauncherMetaTask(Config.LauncherMeta config, ScheduledExecutorService scheduler, OkHttpClient client, List<Notifier> notifiers) {
        this.config = config;
        this.scheduler = scheduler;
        this.client = client;
        this.notifiers = notifiers;

        loadCache();
    }

    private void loadCache() {
        try {
            Path cachePath = Path.of(config.cacheFile());
            cachePath.toFile().createNewFile();
            try (BufferedReader reader = Files.newBufferedReader(cachePath)) {
                JSONParser parser = new JSONParser();
                JSONObject root = (JSONObject) parser.parse(reader);
                JSONArray knownVersionsArray = (JSONArray) root.get("knownVersions");
                knownVersions.addAll(knownVersionsArray);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void saveCache() {
        Path cachePath = Path.of(config.cacheFile());
        JSONObject root = new JSONObject();
        JSONArray knownVersionsArray = new JSONArray();
        knownVersionsArray.addAll(knownVersions);
        root.put("knownVersions", knownVersionsArray);

        try (BufferedWriter writer = Files.newBufferedWriter(cachePath)) {
            root.writeJSONString(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void schedule() {
        Duration interval = DurationUtil.parseTime(config.interval());
        scheduler.scheduleAtFixedRate(this::execute, 2000, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void execute() {
        Request request = new Request.Builder()
                .url(config.url())
                .build();

        JSONParser parser = new JSONParser();
        final boolean silent = knownVersions.isEmpty();

        try (Response response = client.newCall(request).execute()) {
            JSONArray versions = (JSONArray) ((JSONObject) parser.parse(response.body().charStream())).get("versions");
            System.out.println("Fetched " + versions.size() + " versions from LauncherMeta");
            versions.stream().filter(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                return !knownVersions.contains(id);
            }).forEach(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                String type = (String) ((JSONObject) entry).get("type");
                knownVersions.add(id);
                if (!silent) {
                    notifiers.forEach(notifier -> notifier.notifyLauncherMeta(id, type));
                }
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        scheduler.schedule(this::saveCache, 10, TimeUnit.MILLISECONDS);
    }
}
