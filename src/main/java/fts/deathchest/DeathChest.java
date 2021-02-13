package fts.deathchest;

import fts.FunctionalToolSet;
import fts.ResourceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class DeathChest implements Listener {
    private static final HashMap<Location, UUID> messageArmor = new HashMap<>();
    private static final HashMap<Location, UUID> owner = new HashMap<>();
    private static final HashMap<Location, Integer> storeLevel = new HashMap<>();
    private static final HashMap<Location, Float> storeExperience = new HashMap<>();
    private static boolean storeExp;
    private static boolean showMeesage;
    private static boolean onlyOwnerCanOpen;

    public static void initialize(FunctionalToolSet plugin) {
        ResourceUtils.autoUpdateConfigs("deathchest.yml");
        File file = new File(plugin.getDataFolder(), "deathchest.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.getBoolean("enable")) {
            return;
        }
        storeExp = yaml.getBoolean("storeExp");
        showMeesage = yaml.getBoolean("showMessage");
        onlyOwnerCanOpen = yaml.getBoolean("onlyOwnerCanOpen");

        Bukkit.getPluginManager().registerEvents(new DeathChest(), plugin);
    }

    private static boolean hasNull(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                return true;
            }
            if (inv.getItem(i).getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission("fts.deathchest.use")) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Location dieLoc = player.getLocation().getBlock().getLocation().clone().add(0, 1, 0);
                if (dieLoc.getY() <= 0) {
                    for (int i = 128; i > 0; i--) {
                        dieLoc.setY(i);
                        if (dieLoc.getBlock().getType() != Material.AIR) {
                            dieLoc.add(0, 2, 0);
                            break;
                        }
                    }
                }

                if (dieLoc.getBlock().getType() != Material.AIR) {
                    player.sendMessage("您的死亡地点无法布置箱子，故无法生成死亡掉落存储箱！");
                    return;
                }

                BlockBreakEvent event2 = new BlockBreakEvent(dieLoc.getBlock(), player);
                Bukkit.getPluginManager().callEvent(event2);
                if (event2.isCancelled()) {
                    player.sendMessage("您的死亡地点无法布置箱子，故无法生成死亡掉落存储箱！");
                    return;
                }

                dieLoc.getBlock().setType(Material.CHEST);

                Chest chest = (Chest) dieLoc.getBlock().getState();
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null) {
                        continue;
                    }
                    if (hasNull(chest.getInventory())) {
                        chest.getInventory().addItem(item.clone());
                    } else {
                        dieLoc.getWorld().dropItemNaturally(dieLoc, item);
                    }
                }

                event.getDrops().clear();
                event.setDroppedExp(0);
                player.getInventory().clear();
                owner.put(dieLoc, player.getUniqueId());

                if (showMeesage) {
                    ArmorStand armor = (ArmorStand) dieLoc.getWorld().spawnEntity(dieLoc.clone().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
                    armor.setVisible(false);
                    armor.setCustomNameVisible(true);
                    armor.setCustomName("§e玩家§r" + player.getName() + "§e死前掉落的遗物，打开箱子以获取");
                    messageArmor.put(dieLoc, armor.getUniqueId());

                    new BukkitRunnable() {
                        int counter = 0;

                        @Override
                        public void run() {
                            counter++;
                            if (counter >= 3600 || dieLoc.getBlock().getType() != Material.CHEST) {
                                messageArmor.remove(dieLoc);
                                armor.remove();
                                cancel();
                                return;
                            }
                        }
                    }.runTaskTimer(FunctionalToolSet.getInstance(), 0L, 20L);
                }

                if (storeExp) {
                    storeLevel.put(dieLoc, player.getLevel());
                    storeExperience.put(dieLoc, player.getExp());
                    player.setLevel(0);
                    player.setExp(0);
                }

                player.sendMessage("你的物品和经验掉落于x:§e" + dieLoc.getBlockX() + "§r z:§e" + dieLoc.getBlockZ());
            }
        }.runTaskLater(FunctionalToolSet.getInstance(), 2L);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getInventory() == null) {
            return;
        }
        Inventory inv = event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null) {
            return;
        }
        if (owner.containsKey(loc)) {
            if (onlyOwnerCanOpen && owner.get(loc) != player.getUniqueId() && !player.hasPermission("fts.deathchest.ignorewho")) {
                event.setCancelled(true);
                player.sendMessage("这个箱子只有死者本人才能打开！");
                return;
            }
            if (storeExp) {
                player.setLevel(storeLevel.get(loc));
                player.setExp(storeExperience.get(loc));
                storeLevel.remove(loc);
                storeExperience.remove(loc);
            }
            if (showMeesage) {
                Entity entity = Bukkit.getEntity(messageArmor.get(loc));
                entity.remove();
            }
            owner.remove(loc);
        }
    }
}
