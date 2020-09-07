package io.github.md678685.mojirawatch.notifiers;

@FunctionalInterface
public interface Notifier {

    void notifyNewVersion(String name, String description);

}
