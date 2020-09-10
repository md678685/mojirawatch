package io.github.md678685.mojirawatch.notifiers;

public class SysoutNotifier implements Notifier {
    @Override
    public void notifyJira(String name, String description) {
        System.out.println("New JIRA version '" + name + "' with description: " + description);
    }

    @Override
    public void notifyLauncherMeta(String name, String description) {
        System.out.println("New launcher version '" + name + "' with description: " + description);
    }
}
