package utes.randomcredit;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.permission.Permission;
import utes.UntilTheEndServer;

public class RandomCredits {
	private static Permission vaultPermission = null;
	private static HashMap<String, String> permissionGroups = new HashMap<String, String>();
	private static HashMap<String, String> commandGroups = new HashMap<String, String>();
	private static HashMap<String, Double> percents = new HashMap<String, Double>();
	private static double totalPercents = 0.0;
	private static YamlConfiguration yaml;
	
	public RandomCredits() {
		if (!initVault()) {
			UntilTheEndServer.getInstance().getLogger().severe("Vault无法启动");
			return;
		}
		File file = new File(UntilTheEndServer.getInstance().getDataFolder(), "randomcredits.yml");
		if (!file.exists())
			UntilTheEndServer.getInstance().saveResource("randomcredits.yml", false);
		yaml = YamlConfiguration.loadConfiguration(file);
		
		for (String path : yaml.getKeys(false)) {
			String message = yaml.getString(path + ".message");
			double percent = yaml.getDouble(path + ".percent");
			if (yaml.contains(path + ".permission")) {
				String permission = yaml.getString(path + ".permission");
				initPermission(permission, message, percent);
			} else {
				String command = yaml.getString(path + ".command");
				initCommand(command, message, percent);
			}
			totalPercents += percent;
		}
	}

	public static void initPermission(String permission, String message, double percent) {
		permissionGroups.put(permission, message);
		percents.put(permission, percent);
	}

	public static void initCommand(String command, String message, double percent) {
		commandGroups.put(command, message);
		percents.put(command, percent);
	}

	public static void goRandomPermission(CommandSender sender,Player player) {
		if (!sender.hasPermission("utes.randomcredit")) {
			sender.sendMessage("您没有权限抽取随机权限");
			return;
		}
		if(player==null) {
			sender.sendMessage("玩家不存在或不在线");
			return;
		}
		String permission = "";
		while (permission.equalsIgnoreCase("")) {
			int random = (int) (Math.random() * (permissionGroups.size() - 1));
			String index = (String) permissionGroups.keySet().toArray()[random];
			double percent = percents.get(index);
			if (Math.random() <= percent / totalPercents) {
				permission = index;
			}
		}
		vaultPermission.playerAdd(player, permission);
		String message = permissionGroups.get(permission);
		Bukkit.broadcastMessage(message.replace("%player%", player.getName()).replace("%permission%", permission));
	}

	public static void goRandomCommand(CommandSender sender,Player player) {
		if (!sender.hasPermission("utes.randomcredit")) {
			sender.sendMessage("您没有权限抽取随机指令");
			return;
		}
		if(player==null) {
			sender.sendMessage("玩家不存在或不在线");
			return;
		}
		String cmd = "";
		while (cmd.equalsIgnoreCase("")) {
			int random = (int) (Math.random() * (commandGroups.size() - 1));
			String index = (String) commandGroups.keySet().toArray()[random];

			double percent = percents.get(index);
			if (Math.random() <= percent / totalPercents) {
				cmd = index;
			}
		}
		String message = commandGroups.get(cmd);
		Bukkit.broadcastMessage(message.replace("%player%", player.getName()));
		if (player.isOp())
			player.performCommand(cmd.replace("[空格]", " ").replace("[玩家]", player.getName()));
		else {
			player.setOp(true);
			player.performCommand(cmd.replace("[空格]", " ").replace("[玩家]", player.getName()));
			player.setOp(false);
		}
	}

	private static boolean initVault() {
		boolean hasNull = false;
		RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager()
				.getRegistration(Permission.class);
		if (permissionProvider != null) {
			if ((vaultPermission = permissionProvider.getProvider()) == null)
				hasNull = true;
		}
		return !hasNull;
	}
}