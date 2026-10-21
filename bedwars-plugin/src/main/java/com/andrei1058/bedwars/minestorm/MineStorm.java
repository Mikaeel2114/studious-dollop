package com.andrei1058.bedwars.minestorm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import java.util.logging.Logger;

/**
 * MineStorm branding helpers (aqua &b theme).
 */
public final class MineStorm {

    public static final String NAME = "MineStorm";
    /** Chat prefix: &b&lMineStorm &8» &7 */
    public static final String PREFIX = ChatColor.AQUA + "" + ChatColor.BOLD + NAME + " " + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY;
    /** Console prefix used for colored console messages. */
    public static final String CONSOLE_PREFIX = ChatColor.DARK_AQUA + "[" + ChatColor.AQUA + NAME + ChatColor.DARK_AQUA + "] " + ChatColor.AQUA;

    private MineStorm() {
    }

    public static String color(String s) {
        return s == null ? null : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String msg(String s) {
        return PREFIX + color(s);
    }

    /** Pretty startup banner in the console. */
    public static void banner(Logger logger, String version) {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        if (c == null) {
            logger.info("MineStorm BedWars v" + version + " enabled.");
            return;
        }
        c.sendMessage(ChatColor.DARK_AQUA + "========================================");
        c.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "   MineStorm " + ChatColor.WHITE + "" + ChatColor.BOLD + "BedWars");
        c.sendMessage(ChatColor.AQUA + "   Version: " + ChatColor.WHITE + version);
        c.sendMessage(ChatColor.AQUA + "   Server: " + ChatColor.WHITE + "MineStorm Network");
        c.sendMessage(ChatColor.DARK_AQUA + "========================================");
    }

    public static void console(String message) {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        if (c != null) {
            c.sendMessage(CONSOLE_PREFIX + color(message));
        } else {
            Bukkit.getLogger().info("[" + NAME + "] " + ChatColor.stripColor(color(message)));
        }
    }
}
