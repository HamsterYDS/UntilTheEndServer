package utes.checkplayer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import utes.UntilTheEndServer;
import utes.api.UTEInvHolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class CheckInventory implements Listener {
    private static HashMap<Inventory, UUID> owners = new HashMap<>();

    public static void initialize(UntilTheEndServer plugin) {
        Bukkit.getPluginManager().registerEvents(new CheckInventory(), plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    save(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L);
    }

    private static void save(Player player) {
        PlayerInventory inventory = player.getInventory();
        Inventory enderchest = player.getEnderChest();

        File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/checkinv/", player.getUniqueId().toString() + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                continue;
            }
            yaml.set("inventory." + i, inventory.getItem(i));
        }
        for (int i = 0; i < enderchest.getSize(); i++) {
            if (enderchest.getItem(i) == null) {
                continue;
            }
            yaml.set("enderchest." + i, enderchest.getItem(i));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            UntilTheEndServer.getInstance().getLogger().info("存储玩家" + player.getName() + "背包时发生错误！");
        }
    }

    public static Inventory getInv(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        if (!player.hasPlayedBefore()) {
            return null;
        }
        if (player.isOnline()) {
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            return onlinePlayer.getInventory();
        }

        Inventory inv = Bukkit.createInventory(new HolderCheckInventoryGui(), 54, "玩家" + player.getName() + "的背包");

        File changeFile = new File(UntilTheEndServer.getInstance().getDataFolder() + "/changeinv/", player.getUniqueId().toString() + ".yml");
        if (changeFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(changeFile);
            for (int i = 0; i < 54; i++) {
                if (!yaml.contains("inventory." + i)) {
                    continue;
                }
                ItemStack item = yaml.getItemStack("inventory." + i);
                inv.setItem(i, item);
            }
        } else {
            File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/checkinv/", player.getUniqueId().toString() + ".yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < 54; i++) {
                if (yaml.contains("inventory." + i)) {
                    ItemStack item = yaml.getItemStack("inventory." + i);
                    inv.setItem(i, item);
                }
            }
        }
        owners.put(inv, player.getUniqueId());
        return inv;
    }

    public static Inventory getEnderChest(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        if (!player.hasPlayedBefore()) {
            return null;
        }
        if (player.isOnline()) {
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            return onlinePlayer.getEnderChest();
        }

        Inventory inv = Bukkit.createInventory(new HolderCheckInventoryGui(), 27, "玩家" + player.getName() + "的末影箱");

        File changeFile = new File(UntilTheEndServer.getInstance().getDataFolder() + "/changeinv/", player.getUniqueId().toString() + ".yml");
        if (changeFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(changeFile);
            for (int i = 0; i < 27; i++) {
                if (!yaml.contains("enderchest." + i)) {
                    continue;
                }
                ItemStack item = yaml.getItemStack("enderchest." + i);
                inv.setItem(i, item);
            }
        } else {
            File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/checkinv/", player.getUniqueId().toString() + ".yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < 27; i++) {
                if (yaml.contains("enderchest." + i)) {
                    ItemStack item = yaml.getItemStack("enderchest." + i);
                    inv.setItem(i, item);
                }
            }
        }
        owners.put(inv, player.getUniqueId());
        return inv;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof HolderCheckInventoryGui) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(owners.get(inv));
            File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/changeinv/", player.getUniqueId().toString() + ".yml");
            file.delete();
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (inv.getSize() == 27) {
                for (int i = 0; i < inv.getSize(); i++) {
                    if (inv.getItem(i) == null) {
                        continue;
                    }
                    yaml.set("enderchest." + i, inv.getItem(i));
                }
            } else {
                for (int i = 0; i < inv.getSize(); i++) {
                    if (inv.getItem(i) == null) {
                        continue;
                    }
                    yaml.set("inventory." + i, inv.getItem(i));
                }
            }
            owners.remove(player.getUniqueId());
            try {
                yaml.save(file);
            } catch (IOException e) {
                UntilTheEndServer.getInstance().getLogger().info("存储玩家" + player.getName() + "背包时发生错误！");
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/changeinv/", player.getUniqueId().toString() + ".yml");
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yaml.contains("inventory")) {
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.getSize(); i++) {
                    if (!yaml.contains("inventory." + i)) {
                        continue;
                    }
                    ItemStack item = yaml.getItemStack("inventory." + i);
                    inv.setItem(i, item);
                }
                player.updateInventory();
            }
            if (yaml.contains("enderchest")) {
                Inventory inv = player.getEnderChest();
                for (int i = 0; i < inv.getSize(); i++) {
                    if (!yaml.contains("enderchest." + i)) {
                        continue;
                    }
                    ItemStack item = yaml.getItemStack("enderchest." + i);
                    inv.setItem(i, item);
                }
            }
            file.delete();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        save(event.getPlayer());
        File file = new File(UntilTheEndServer.getInstance().getDataFolder() + "/changeinv/", event.getPlayer().getUniqueId().toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    private static class HolderCheckInventoryGui implements UTEInvHolder {
        public static final HolderCheckInventoryGui INSTANCE = new HolderCheckInventoryGui();
        private String name;

        @Override
        public String getCustomName() {
            return name;
        }

        @Override
        public void setCustomName(String name) {
            this.name = name;
        }
    }
}
