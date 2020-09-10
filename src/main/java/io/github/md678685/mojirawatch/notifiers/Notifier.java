package io.github.md678685.mojirawatch.notifiers;

public interface Notifier {

    void notifyJira(String name, String description);

    void notifyLauncherMeta(String name, String description);
}
