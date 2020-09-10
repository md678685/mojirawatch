package io.github.md678685.mojirawatch;

import com.google.common.reflect.TypeToken;
import io.github.md678685.mojirawatch.notifiers.IrcNotifier;
import io.github.md678685.mojirawatch.notifiers.Notifier;
import io.github.md678685.mojirawatch.notifiers.SysoutNotifier;
import io.github.md678685.mojirawatch.tasks.LauncherMetaTask;
import io.github.md678685.mojirawatch.tasks.JiraTask;
import io.github.md678685.mojirawatch.tasks.Task;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class App {

    private OkHttpClient client = new OkHttpClient();
    private List<Notifier> notifiers = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Config config;

    public void start() {
        loadConfig();
        setupTasks();
        setupNotifiers();
        schedule();
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

    private void setupTasks() {
        tasks.add(new JiraTask(config.jira(), scheduler, client, notifiers));
        tasks.add(new LauncherMetaTask(config.launcherMeta(), scheduler, client, notifiers));
    }

    private void setupNotifiers() {
        notifiers.add(new SysoutNotifier());
        config.notifiers().irc().forEach(ircConfig -> notifiers.add(new IrcNotifier(ircConfig)));
    }

    private void schedule() {
        tasks.forEach(Task::schedule);
    }
}
