package io.github.md678685.mojirawatch.notifiers;

import io.github.md678685.mojirawatch.Config;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.NickServ;

public class IrcNotifier implements Notifier {

    private final Client client;
    private final Config config;

    public IrcNotifier(Config config) {
        this.client = Client.builder()
                .nick(config.getIrcNick())
                .server()
                .host(config.getIrcHost())
                .port(config.getIrcPort(), Client.Builder.Server.SecurityType.SECURE)
                .then().build();
        this.config = config;

        this.client.getAuthManager().addProtocol(
                NickServ.builder(client).account(config.getIrcAuthUser()).password(config.getIrcAuthPassword()).build()
        );

        this.client.connect();
        this.client.addChannel(config.getIrcChannel());
    }

    @Override
    public void notifyNewVersion(String name, String description) {
        String message = String.format("[MOJIRAWATCH] Found new version '%s' with description '%s'!", name, description);
        this.client.sendMessage(config.getIrcChannel(), message);
    }
}
