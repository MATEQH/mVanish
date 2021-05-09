package me.matthew.vanish;

import lombok.Getter;
import me.matthew.vanish.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VanishPlugin extends JavaPlugin {

    @Getter
    private VanishPlugin instance;
    @Getter
    private Map<String, Integer> priorityMap;
    @Getter
    private List<Player> vanished;
    @Getter
    private BukkitTask task;
    @Getter
    private boolean running = true;

    @Override
    public void onEnable() {
        instance = this;
        priorityMap = new HashMap<>();
        vanished = new ArrayList<>();
        saveResource("config.yml", false);
        getConfig().getConfigurationSection("priority").getKeys(false).forEach(key -> priorityMap.put(getConfig().getString("priority." + key + ".permission"), getConfig().getInt("priority." + key + ".value")));
        priorityMap = priorityMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> k, LinkedHashMap::new));
        getCommand("vanish").setExecutor(this);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() > 1) {
                    Bukkit.getOnlinePlayers().forEach(target -> Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player != target) {
                            int playerPriority = getPriority(player), targetPriority = getPriority(target);
                            ReflectionUtil.postToMainThread(() -> {
                                if (vanished(player) && vanished(target)) {
                                    if (playerPriority < targetPriority) {
                                        player.hidePlayer(target);
                                    } else {
                                        player.showPlayer(target);
                                    }
                                } else if (vanished(player) && !vanished(target)) {
                                    target.hidePlayer(player);
                                    player.showPlayer(target);
                                } else if (!vanished(player) && vanished(target)) {
                                    player.hidePlayer(target);
                                    target.showPlayer(player);
                                } else {
                                    target.showPlayer(player);
                                    player.showPlayer(target);
                                }
                            });
                        }
                    }));
                }
            }
        }.runTaskTimerAsynchronously(this, 20l, 4l);
    }

    @Override
    public void onDisable() {
        running = false;
        task.cancel();
        Bukkit.getOnlinePlayers().forEach(target -> Bukkit.getOnlinePlayers().forEach(player -> {
            if (player != target) {
                target.showPlayer(player);
                player.showPlayer(target);
            }
        }));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(getConfig().getString("permission.use"))) {
                player.sendMessage(getMessage("no-permission"));
                return false;
            }
            if (args.length == 0) {
                vanish(player, !vanished(player));
                if (vanished(player)) {
                    player.sendMessage(getMessage("enabled"));
                } else {
                    player.sendMessage(getMessage("disabled"));
                }
            } else if (args.length == 1) {
                if (!player.hasPermission(getConfig().getString("permission.use-other"))) {
                    player.sendMessage(getMessage("no-permission"));
                    return false;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(getMessage("target-not-found").replace("%targetName%", args[0]));
                    return false;
                }
                vanish(target, !vanished(target));
                if (vanished(target)) {
                    target.sendMessage(getMessage("enabled-target").replace("%executorName%", player.getName()));
                    player.sendMessage(getMessage("enabled-executor").replace("%targetName%", target.getName()));
                } else {
                    target.sendMessage(getMessage("disabled-target").replace("%executorName%", player.getName()));
                    player.sendMessage(getMessage("disabled-executor").replace("%targetName%", target.getName()));
                }
            } else {
                player.sendMessage(getMessage("usage").replace("%label%", label));
            }
        } else if (sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(getMessage("target-not-found").replace("%targetName%", args[0]));
                    return false;
                }
                vanish(target, !vanished(target));
                if (vanished(target)) {
                    target.sendMessage(getMessage("enabled-target").replace("%executorName%", "Console"));
                    sender.sendMessage(getMessage("enabled-executor").replace("%targetName%", target.getName()));
                } else {
                    target.sendMessage(getMessage("disabled-target").replace("%executorName%", "Console"));
                    sender.sendMessage(getMessage("disabled-executor").replace("%targetName%", target.getName()));
                }
            } else {
                sender.sendMessage(getMessage("usage").replace("%label%", label));
            }
        }
        return true;
    }

    /**
     * Returns the priority what the player have
     * @param player the player
     * @return player priority or -1 if not specified
     */
    public int getPriority(Player player) {
        for (String key : priorityMap.keySet()) {
            if (player.hasPermission(key)) {
                return priorityMap.get(key);
            }
        }
        return -1;
    }

    /**
     * Toggle vanish for the player
     * @param player the player
     * @param toggle boolean value for toggle (true/false)
     */
    public void vanish(Player player, boolean toggle) {
        if (toggle && !vanished(player)) {
            vanished.add(player);
        } else if (!toggle && vanished(player)) {
            vanished.remove(player);
        }
    }

    /**
     * Check vanish is enabled for the player
     * @param player the player
     * @return
     */
    public boolean vanished(Player player) {
        return vanished.contains(player);
    }

    /**
     * Returns the message from config.yml by path
     * @param path location of message
     * @return the message or null
     * @throws NullPointerException if message cannot be found
     */
    private String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("message." + path));
    }
}
