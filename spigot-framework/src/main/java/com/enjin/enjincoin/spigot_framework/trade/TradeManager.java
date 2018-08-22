package com.enjin.enjincoin.spigot_framework.trade;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.event.MinecraftPlayerQuitEvent;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TradeManager implements Listener {

    private BasePlugin plugin;

    public TradeManager(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean inviteExists(MinecraftPlayer sender, MinecraftPlayer target) {
        return sender.getSentTradeInvites().contains(target);
    }

    public boolean addInvite(MinecraftPlayer sender, MinecraftPlayer target) {
        boolean result = !inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().add(target);
            target.getReceivedTradeInvites().add(sender);
        }

        return result;
    }

    public boolean acceptInvite(MinecraftPlayer sender, MinecraftPlayer target) {
        boolean result = inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().remove(target);
            target.getReceivedTradeInvites().remove(target);

            sender.setActiveTradeView(new TradeView(sender, target));
            target.setActiveTradeView(new TradeView(target, sender));
        }

        return result;
    }

    @EventHandler
    public void onMinecraftPlayerQuit(MinecraftPlayerQuitEvent event) {
        MinecraftPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        TradeView tradeView = player.getActiveTradeView();
        if (tradeView != null && tradeView.getOther() != null) {
            player.setActiveTradeView(null);
            tradeView.destroy();

            MinecraftPlayer other = tradeView.getOther();
            if (other != null) {
                TradeView otherTradeView = other.getActiveTradeView();
                if (otherTradeView != null) {
                    if (otherTradeView != null) {
                        otherTradeView.removePlayer(other.getBukkitPlayer());
                        otherTradeView.destroy();
                    }
                }
            }
        }
    }

}