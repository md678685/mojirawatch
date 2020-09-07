package io.github.md678685.mojirawatch.notifiers;

import io.github.md678685.mojirawatch.Config;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.kitteh.irc.client.library.feature.auth.SaslPlain;

public class IrcNotifier implements Notifier {

    private final Client client;
    private final Config.Irc config;

    public IrcNotifier(Config.Irc config) {
        this.config = config;

        this.client = Client.builder()
                .nick(config.nick())
                .server()
                .host(config.host())
                .port(config.port(), Client.Builder.Server.SecurityType.SECURE)
                .then().build();

        switch (config.auth().type()) {
            case NICKSERV:
                this.client.getAuthManager().addProtocol(
                        NickServ.builder(client).account(config.auth().username()).password(config.auth().password()).build()
                );
                break;
            case SASL:
                this.client.getAuthManager().addProtocol(
                        new SaslPlain(client, config.auth().username(), config.auth().password())
                );
            case NONE:
            default:
                break;
        }
        this.client.connect();
        this.config.channels().forEach(client::addChannel);
    }

    @Override
    public void notifyNewVersion(String name, String description) {
        String message = String.format("[MOJIRAWATCH] Found new version '%s' with description '%s'!", name, description);
        this.config.channels().forEach(channel -> client.sendMessage(channel, message));
    }
}
