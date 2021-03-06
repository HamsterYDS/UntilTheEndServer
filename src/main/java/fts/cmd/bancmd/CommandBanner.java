package fts.cmd.bancmd;

import fts.FunctionalToolSet;
import fts.spi.ResourceUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class CommandBanner implements Listener {
    private static final HashMap<String, List<String>> worlds = new HashMap<>();

    public static void initialize(FunctionalToolSet plugin) {
        ResourceUtils.autoUpdateConfigs("cmdban.yml");
        File file = new File(plugin.getDataFolder(), "cmdban.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.getBoolean("enable")) {
            return;
        }

        for (String path : yaml.getKeys(false)) {
            if (path.equalsIgnoreCase("enable")) {
                continue;
            }
            worlds.put(path, yaml.getStringList(path));
        }

        Bukkit.getPluginManager().registerEvents(new CommandBanner(), plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("fts.cmdban.ignore")) {
            return;
        }
        World world = player.getWorld();
        String cmd = event.getMessage();
        while (cmd.startsWith(" ")) {
            cmd = cmd.replaceFirst(" ", "");
        }
        while (cmd.startsWith("/ ")) {
            cmd = cmd.replaceFirst("/ ", "/");
        }
        if (!worlds.containsKey(world.getName())) {
            return;
        }
        for (String label : worlds.get(world.getName())) {
            if (cmd.startsWith("/" + label)) {
                event.setCancelled(true);
                ResourceUtils.sendMessage(player, "ban-cmd");
            }
        }
    }
}
