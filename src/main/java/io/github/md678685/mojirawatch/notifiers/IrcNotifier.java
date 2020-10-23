package io.github.md678685.mojirawatch.notifiers;

import io.github.md678685.mojirawatch.Config;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.kitteh.irc.client.library.feature.auth.SaslPlain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IrcNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(IrcNotifier.class);

    private final Client client;
    private final Config.Irc config;

    public IrcNotifier(Config.Irc config) {
        this.config = config;

        this.client = Client.builder()
                .nick(config.nick())
                .user(config.user())
                .realName(config.realname())
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
        this.client.getEventManager().registerEventListener(this);
        this.client.connect();
        this.joinAllChannels();
    }

    @Override
    public void notifyJira(String name, String description) {
        String message = String.format("[MOJIRAWATCH] Found new version '%s' with description '%s'!", name, description);
        this.config.channels().forEach(channel -> client.sendMessage(channel, message));
    }

    @Override
    public void notifyLauncherMeta(String name, String type) {
        String message = String.format("[LAUNCHERWATCH] Found new version '%s' of type '%s'!", name, type);
        this.config.channels().forEach(channel -> client.sendMessage(channel, message));
    }

    @Handler
    public void onConnect(ClientConnectionEstablishedEvent event) {
        LOGGER.info(String.format("Connection to IRC established [%s:%d]", this.config.host(), this.config.port()));
        this.joinAllChannels();
    }

    private void joinAllChannels() {
        this.config.channels().forEach(client::addChannel);
    }

}
