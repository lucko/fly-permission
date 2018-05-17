package me.lucko.flypermission;

import com.google.common.base.Strings;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class FlyPermission extends JavaPlugin implements Listener {

    private String permission;
    private String disableMessage;
    private Set<GameMode> ignoredGamemodes;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        this.permission = config.getString("permission", "fly.use");
        this.disableMessage = Strings.emptyToNull(config.getString("disable-message", null));
        if (this.disableMessage != null) {
            this.disableMessage = ChatColor.translateAlternateColorCodes('&', this.disableMessage);
        }
        this.ignoredGamemodes = EnumSet.noneOf(GameMode.class);
        for (String ignoredGamemode : config.getStringList("ignored-gamemodes")) {
            try {
                this.ignoredGamemodes.add(GameMode.valueOf(ignoredGamemode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Unknown gamemode: " + ignoredGamemode);
            }
        }

        String mode = config.getString("checking-mode", "move-event").toLowerCase();
        switch (mode) {
            case "move-event":
                getLogger().info("Using move event for detection");
                getServer().getPluginManager().registerEvents(this, this);
                break;
            case "timer":
                getLogger().info("Using timer for detection");
                long interval = config.getLong("timer-interval", 2);
                getServer().getScheduler().runTaskTimer(this, this::checkTimer, interval, interval);
                break;
            default:
                throw new RuntimeException("Unknown checking mode: " + mode);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkMove(PlayerMoveEvent e) {
        Location f = e.getFrom();
        Location t = e.getTo();

        // only run a check if they moved over a block
        if (f.getBlockX() != t.getBlockX() || f.getBlockZ() != t.getBlockZ() || f.getBlockY() != t.getBlockY() || !f.getWorld().equals(t.getWorld())) {
            check(e.getPlayer());
        }
    }

    private void checkTimer() {
        for (Player player : getServer().getOnlinePlayers()) {
            check(player);
        }
    }

    private void check(Player player) {
        if (!player.getAllowFlight()) {
            return;
        }
        if (this.ignoredGamemodes.contains(player.getGameMode())) {
            return;
        }
        if (player.hasPermission(this.permission)) {
            return;
        }

        // send disable message
        if (this.disableMessage != null) {
            player.sendMessage(this.disableMessage);
        }

        // disable flight
        player.setAllowFlight(false);
    }
}
