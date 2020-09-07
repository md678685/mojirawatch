package io.github.md678685.mojirawatch;

import com.google.common.reflect.TypeToken;
import io.github.md678685.mojirawatch.notifiers.IrcNotifier;
import io.github.md678685.mojirawatch.notifiers.Notifier;
import io.github.md678685.mojirawatch.util.DurationUtil;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    private OkHttpClient client = new OkHttpClient();
    private List<Notifier> notifiers = new ArrayList<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Config config;
    private List<String> knownVersions = new ArrayList<>();

    public void start() {
        loadConfig();
        loadCache();
        setupNotifiers();

        Duration interval = DurationUtil.parseTime(config.jira().interval());
        scheduler.scheduleAtFixedRate(this::pollJira, 2000, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void loadConfig() {
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setPath(Path.of("./config.conf"))
                .build();

        try {
            config = loader.load().getValue(TypeToken.of(Config.class));
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void loadCache() {
        try {
            Path cachePath = Path.of(config.jira().cacheFile());
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

    private void setupNotifiers() {
        notifiers.add(((name, description) -> System.out.println("New version '" + name + "' with description: " + description)));
        config.notifiers().irc().forEach(ircConfig -> notifiers.add(new IrcNotifier(ircConfig)));
    }

    private void saveCache() {
        Path cachePath = Path.of(config.jira().cacheFile());
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

    private void pollJira() {
        Request request = new Request.Builder()
                .url(config.jira().url())
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
