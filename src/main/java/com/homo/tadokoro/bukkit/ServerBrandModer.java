package com.homo.tadokoro.bukkit;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ServerBrandModer extends JavaPlugin implements Listener {

    @ChannelHandler.Sharable
    static class Interceptor extends ChannelDuplexHandler {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ClientboundCustomPayloadPacket packet && packet.payload() instanceof BrandPayload) {
                super.write(ctx, new ClientboundCustomPayloadPacket(new BrandPayload(SERVER_BRAND_NAME)), promise);
                return;
            }
            super.write(ctx, msg, promise);
        }
    }

    private static final Interceptor interceptor = new Interceptor();
    private static String SERVER_BRAND_NAME;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SERVER_BRAND_NAME = getConfig().getString("server-brand-name");
        Bukkit.getPluginManager().registerEvents(this, this);
        ChannelInitializeListenerHolder.addListener(Key.key("brand_handler"), channel -> channel.pipeline().addBefore("packet_handler", "brand_handler", interceptor));
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent e) {
        int index = e.getVersion().lastIndexOf(' ');
        e.setVersion(SERVER_BRAND_NAME + e.getVersion().substring(index));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !sender.hasPermission("serverbrand.modify")) {
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            SERVER_BRAND_NAME = getConfig().getString("server-brand-name");
            sender.sendMessage(ChatColor.GREEN + "Reload the server brand name to " + SERVER_BRAND_NAME);
            sender.sendMessage(ChatColor.GREEN + "You can find the change by re-join the server");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
            StringBuilder newBrandName = new StringBuilder(args[1]);
            for (int i=2; i<args.length; i++) {
                newBrandName.append(' ').append(args[i]);
            }
            SERVER_BRAND_NAME = newBrandName.toString();
            getConfig().set("server-brand-name", SERVER_BRAND_NAME);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Set the server brand name to " + SERVER_BRAND_NAME);
            sender.sendMessage(ChatColor.GREEN + "You can find the change by re-join the server");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return args.length == 1 ? List.of("reload", "set") : Collections.emptyList();
    }
}