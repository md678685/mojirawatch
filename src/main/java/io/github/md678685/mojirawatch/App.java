package io.github.md678685.mojirawatch;

import io.github.md678685.mojirawatch.notifiers.IrcNotifier;
import io.github.md678685.mojirawatch.notifiers.Notifier;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    private Config config = new Config();
    private OkHttpClient client = new OkHttpClient();
    private List<Notifier> notifiers = List.of(
            new IrcNotifier(config),
            ((name, description) -> System.out.println("New version '" + name + "' with description: " + description))
    );
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private List<String> knownVersions = new ArrayList<>();

    public void start() {
        loadCache();

        scheduler.scheduleAtFixedRate(this::pollJira, 10000000, config.getInterval().toNanos(), TimeUnit.NANOSECONDS);
    }

    private void loadCache() {
        try {
            config.getCacheFile().toFile().createNewFile();
            try (BufferedReader reader = Files.newBufferedReader(config.getCacheFile())) {
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
        JSONObject root = new JSONObject();
        JSONArray knownVersionsArray = new JSONArray();
        knownVersionsArray.addAll(knownVersions);
        root.put("knownVersions", knownVersionsArray);

        try (BufferedWriter writer = Files.newBufferedWriter(config.getCacheFile())) {
            root.writeJSONString(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pollJira() {
        Request request = new Request.Builder()
                .url(config.getProjectUrl())
                .build();

        JSONParser parser = new JSONParser();
        final boolean silent = knownVersions.isEmpty();

        try (Response response = client.newCall(request).execute()) {
            JSONArray versions = (JSONArray) ((JSONObject) parser.parse(response.body().charStream())).get("versions");
            System.out.println("Fetched " + versions.size() + " versions from JIRA");
            versions.stream().filter(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                return !knownVersions.contains(id);
            }).forEach(entry -> {
                String id = (String) ((JSONObject) entry).get("id");
                String name = (String) ((JSONObject) entry).get("name");
                String description = (String) ((JSONObject) entry).get("description");
                knownVersions.add(id);
                if (!silent) {
                    notifiers.forEach(notifier -> notifier.notifyNewVersion(name, description));
                }
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        scheduler.schedule(this::saveCache, 10, TimeUnit.MILLISECONDS);
    }
}
