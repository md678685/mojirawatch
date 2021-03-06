package io.github.md678685.mojirawatch.tasks;

import io.github.md678685.mojirawatch.Config;
import io.github.md678685.mojirawatch.notifiers.Notifier;
import io.github.md678685.mojirawatch.notifiers.SysoutNotifier;
import io.github.md678685.mojirawatch.util.DurationUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class JiraTask implements Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraTask.class);

    private final Config.Jira config;
    private final ScheduledExecutorService scheduler;
    private final OkHttpClient client;
    private final List<Notifier> notifiers;

    private final List<String> knownVersions = new ArrayList<>();

    public JiraTask(Config.Jira config, ScheduledExecutorService scheduler, OkHttpClient client, List<Notifier> notifiers) {
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
            LOGGER.info("Fetched " + versions.size() + " versions from JIRA");
            versions.stream().filter(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                return !knownVersions.contains(id);
            }).forEach(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                String name = (String) ((JSONObject) entry).get("name");
                String description = (String) ((JSONObject) entry).get("description");
                knownVersions.add(id);
                if (!silent) {
                    notifiers.forEach(notifier -> notifier.notifyJira(name, description));
                }
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        scheduler.schedule(this::saveCache, 10, TimeUnit.MILLISECONDS);
    }
}
