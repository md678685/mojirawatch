package io.github.md678685.mojirawatch;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.nio.file.Path;
import java.util.List;

@ConfigSerializable
public class Config {

    @Setting
    private Jira jira;
    @Setting
    private Notifiers notifiers;

    public Jira jira() {
        return jira;
    }

    public Notifiers notifiers() {
        return notifiers;
    }

    @ConfigSerializable
    public static class Jira {
        @Setting
        private String url;
        @Setting("cache-file")
        private String cacheFile;
        @Setting
        private String interval;

        public String url() {
            return url;
        }

        public String cacheFile() {
            return cacheFile;
        }

        public String interval() {
            return interval;
        }
    }

    @ConfigSerializable
    public static class Notifiers {
        @Setting
        private List<Irc> irc;

        public List<Irc> irc() {
            return irc;
        }
    }

    @ConfigSerializable
    public static class Irc {
        @Setting
        private String host;
        @Setting
        private int port;
        @Setting
        private String nick;
        @Setting
        private IrcAuth auth;
        @Setting
        private List<String> channels;

        public String host() {
            return host;
        }

        public int port() {
            return port;
        }

        public String nick() {
            return nick;
        }

        public IrcAuth auth() {
            return auth;
        }

        public List<String> channels() {
            return channels;
        }
    }

    @ConfigSerializable
    public static class IrcAuth {
        @Setting
        private String username;
        @Setting
        private String password;
        @Setting
        private IrcAuthType type;

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        public IrcAuthType type() {
            return type;
        }
    }

    public enum IrcAuthType {
        NICKSERV,
        SASL,
        NONE
    }

}
