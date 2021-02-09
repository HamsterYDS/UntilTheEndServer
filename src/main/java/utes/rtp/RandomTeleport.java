package utes.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import utes.UntilTheEndServer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//TODO
/*
 * utes.rtp.ignorecd
 * utes.rtp.ignoreworld
 * utes.rtp.ignoremove
 * */
public class RandomTeleport {
    private static final List<String> enableWorlds = new ArrayList<>();
    private static final HashMap<UUID, Long> lastUseTimeStamp = new HashMap<>();
    private static int waitTime;
    private static int maxX;
    private static int maxZ;
    private static int minX;
    private static int minZ;
    private static int cooldown;

    public static void initialize(UntilTheEndServer plugin) {
        File file = new File(plugin.getDataFolder(), "rtp.yml");
        if (!file.exists()) {
            plugin.saveResource("rtp.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.getBoolean("enable")) {
            return;
        }

        waitTime = yaml.getInt("waitTime");
        maxX = yaml.getInt("maxRange.x");
        maxZ = yaml.getInt("maxRange.z");
        minX = yaml.getInt("minRange.x");
        minZ = yaml.getInt("minRange.z");
        cooldown = yaml.getInt("cooldown");

        List<String> disableWorlds = yaml.getStringList("disableWorlds");
        for (World world : Bukkit.getWorlds()) {
            if (!disableWorlds.contains(world.getName())) {
                enableWorlds.add(world.getName());
            }
        }
    }

    public static void initRTP(Player player) {
        if ((!enableWorlds.contains(player.getWorld().getName())) && (!player.hasPermission("utes.rtp.ignoreworld"))) {
            player.sendMessage("本世界禁止随机传送！");
            return;
        }
        if (lastUseTimeStamp.containsKey(player.getUniqueId()) && (!player.hasPermission("utes.rtp.ignorecd"))) {
            if (System.currentTimeMillis() - lastUseTimeStamp.get(player.getUniqueId()) < cooldown * 1000) {
                player.sendMessage("传送冷却未到！还有§6"
                        + (cooldown - System.currentTimeMillis() - lastUseTimeStamp.get(player.getUniqueId()) / 1000)
                        + "§r秒");
                return;
            }
        }
        player.sendMessage("等待随机传送中，3s内请不要移动！");
        Location from = player.getLocation();
        Location destination = getRandomDestination(from.clone());
        if (destination == null) {
            player.sendMessage("找不到合适的随机传送点，请重试");
            return;
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (player.getLocation().distance(from) >= 0.3 && (!player.hasPermission("utes.rtp.ignoremove"))) {
                    player.sendMessage("您移动了，随机传送取消！");
                    return;
                }
                player.sendMessage("随机传送至x:§6" + destination.getBlockX() + "§r z:§6"
                        + destination.getBlockZ());
                player.teleport(destination);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0));
                lastUseTimeStamp.remove(player.getUniqueId());
                lastUseTimeStamp.put(player.getUniqueId(), System.currentTimeMillis());
            }

        }.runTaskLater(UntilTheEndServer.getInstance(), waitTime * 20L);
    }

    private static Location getRandomDestination(Location loc) {
        Location current = loc.clone();
        int y = -1, counter = 0;
        do {
            counter++;
            if (counter == 100) {
                break;
            }

            loc = current.clone();
            loc.add(minX + Math.random() * (maxX - minX), 0, minZ + Math.random() * (maxZ - minZ));

            y = isSafe(loc);
            if (y != -1) {
                break;
            }
        } while (true);
        if (y == -1) {
            return null;
        }
        loc.setY(y);
        return loc;
    }

    private static int isSafe(Location loc) {
        for (int y = 128; y >= 32; y--) {
            loc.setY(y);
            Location upLoc = loc.clone().add(0, 1, 0);
            Location downLoc = loc.clone().add(0, -1, 0);
            if (loc.getBlock().getType() == Material.AIR && upLoc.getBlock().getType() == Material.AIR
                    && downLoc.getBlock().getType() != Material.AIR && !downLoc.getBlock().getType().isSolid()) {
                return y;
            }
        }
        return -1;
    }
}
