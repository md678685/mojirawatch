package io.github.md678685.mojirawatch;

import java.nio.file.Path;
import java.time.Duration;

public class Config {

    private final String projectUrl;
    private final Path cacheFile;
    private final Duration interval;

    private final String ircHost;
    private final int ircPort;
    private final String ircNick;
    private final String ircAuthUser;
    private final String ircAuthPassword;
    private final String ircChannel;

    Config() {
        projectUrl = System.getProperty("mojirawatch.url", "https://bugs.mojang.com/rest/api/2/project/MC");
        cacheFile = Path.of(System.getProperty("mojirawatch.cachefile", "./mojiracache.json"));
        interval = Duration.parse(System.getProperty("mojirawatch.interval", "PT10M"));

        ircHost = System.getProperty("mojirawatch.irc.host", "irc.esper.net");
        ircPort = Integer.parseInt(System.getProperty("mojirawatch.irc.port", "6697"));
        ircNick = System.getProperty("mojirawatch.irc.nick", "md6secretbot");
        ircAuthUser = System.getProperty("mojirawatch.irc.auth.user", ircNick);
        ircAuthPassword = System.getProperty("mojirawatch.irc.auth.password", "md6secretbot");
        ircChannel = System.getProperty("mojirawatch.irc.channel", "#mojirawatch");
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public Path getCacheFile() {
        return cacheFile;
    }

    public Duration getInterval() {
        return interval;
    }

    public String getIrcHost() {
        return ircHost;
    }

    public int getIrcPort() {
        return ircPort;
    }

    public String getIrcNick() {
        return ircNick;
    }

    public String getIrcAuthUser() {
        return ircAuthUser;
    }

    public String getIrcAuthPassword() {
        return ircAuthPassword;
    }

    public String getIrcChannel() {
        return ircChannel;
    }
}
