package com.homo.tadokoro.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerEnterConfigurationEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Plugin(
    id = "serverbrandmoder",
    name = "ServerBrandModer",
    version = "1.14.514",
    authors = {"KojiTadokoro114"}
)
public class ServerBrandModer {

    private final ProxyServer server;
    private final ServerBrandHandler brandHandler = new ServerBrandHandler();
    public static String SERVER_BRAND_NAME;
    private static SimpleConfigHandler config;

    @Inject
    public ServerBrandModer(ProxyServer server, Logger logger, @DataDirectory Path configDirectory) {
        this.server = server;
        config = new SimpleConfigHandler(configDirectory.toFile());
        SERVER_BRAND_NAME = config.get("server-brand-name");
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
        server.getEventManager().register(this, ProxyPingEvent.class, event -> {
            ServerPing ping = event.getPing();
            ServerPing.Version version = ping.getVersion();
            int index = version.getName().lastIndexOf(" ");
            ping = ping.asBuilder().version(new ServerPing.Version(
            version.getProtocol(),
            SERVER_BRAND_NAME + version.getName().substring(index))).build();
            event.setPing(ping);
        });
        server.getEventManager().register(this, ServerPreConnectEvent.class, event -> {
            ((ConnectedPlayer) event.getPlayer()).getConnection().getChannel().pipeline().addBefore("handler", "brand_hander", brandHandler);
        });
        ServerBrandCommand.register(server);
    }

    @ChannelHandler.Sharable
    static class ServerBrandHandler extends ChannelDuplexHandler {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof PluginMessagePacket packet && PluginMessageUtil.isMcBrand(packet)) {
                super.write(ctx, rewriteMinecraftBrand(packet), promise);
                return;
            }
            super.write(ctx, msg, promise);
        }

        private static PluginMessagePacket rewriteMinecraftBrand(PluginMessagePacket message) {
            String currentBrand = PluginMessageUtil.readBrandMessage(message.content());
            int index = currentBrand.lastIndexOf("(");
            String rewrittenBrand = currentBrand.substring(0, index) + "(" + SERVER_BRAND_NAME + ")";
            ByteBuf rewrittenBuf = Unpooled.buffer();
            ProtocolUtils.writeString(rewrittenBuf, rewrittenBrand);
            return new PluginMessagePacket(message.getChannel(), rewrittenBuf);
        }
    }

    static class ServerBrandCommand {

        public static void register(ProxyServer server) {
            CommandManager manager = server.getCommandManager();
            CommandMeta meta = manager.metaBuilder("velocityserverbrand").aliases("vsb").build();
            manager.register(meta, new SimpleCommand() {

                @Override
                public void execute(Invocation invocation) {
                    String[] args = invocation.arguments();
                    if (!(invocation.source() instanceof ConsoleCommandSource sender) || args.length == 0) {
                        return;
                    }
                    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                        SERVER_BRAND_NAME = config.get("server-brand-name");
                        sender.sendMessage(Component.text("Reload the server brand name to " + SERVER_BRAND_NAME).color(NamedTextColor.GREEN));
                        sender.sendMessage(Component.text("You can find the change by re-join the server").color(NamedTextColor.GREEN));
                    } else if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
                        StringBuilder newBrandName = new StringBuilder(args[1]);
                        for (int i = 2; i < args.length; i++) {
                            newBrandName.append(' ').append(args[i]);
                        }
                        SERVER_BRAND_NAME = newBrandName.toString();
                        config.set("server-brand-name", SERVER_BRAND_NAME);
                        config.saveConfig();
                        sender.sendMessage(Component.text("Set the server brand name to " + SERVER_BRAND_NAME).color(NamedTextColor.GREEN));
                        sender.sendMessage(Component.text("You can find the change by re-join the server").color(NamedTextColor.GREEN));
                    }
                }

                @Override
                public boolean hasPermission(final Invocation invocation) {
                    return invocation.source().hasPermission("serverbrand.modify");
                }

                @Override
                public List<String> suggest(final Invocation invocation) {
                    return invocation.arguments().length < 2 ? List.of("reload", "set") : Collections.emptyList();
                }
            });
        }
    }
}