package io.github.md678685.mojirawatch.notifiers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysoutNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysoutNotifier.class);

    @Override
    public void notifyJira(String name, String description) {
        LOGGER.info("New JIRA version '" + name + "' with description: " + description);
    }

    @Override
    public void notifyLauncherMeta(String name, String description) {
        LOGGER.info("New launcher version '" + name + "' with description: " + description);
    }
}
