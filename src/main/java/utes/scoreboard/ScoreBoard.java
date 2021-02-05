package utes.scoreboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import utes.UntilTheEndServer;
import utes.onlinetimes.OnlineTimes;

//TODO
/*
 * utes.sb.toggle
 */
public class ScoreBoard implements Listener{
	private static ArrayList<UUID> disablers = new ArrayList<UUID>();
	private static String title;
	private static List<String> lines;
	private static YamlConfiguration yaml;

	public ScoreBoard() {
		File file = new File(UntilTheEndServer.getInstance().getDataFolder(), "scoreboard.yml");
		if (!file.exists())
			UntilTheEndServer.getInstance().saveResource("scoreboard.yml", false);
		yaml = YamlConfiguration.loadConfiguration(file);
		if(!yaml.getBoolean("enable")){
			return;
		}

		title = yaml.getString("title");
		lines = yaml.getStringList("lines");
		new BukkitRunnable() {
			@Override
			public void run() {
				for (World world : Bukkit.getWorlds()) {
					for (Player player : world.getPlayers()) {
						if (disablers.contains(player.getUniqueId()))
							continue;
						Scoreboard board = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
						Objective object = board.registerNewObjective("Scoreboard", "scoreboard");
						object.setDisplayName(PlaceholderAPI.setPlaceholders(player, title));
						object.setDisplaySlot(DisplaySlot.SIDEBAR);
						int size = lines.size();
						for (String line : lines) {
							line = PlaceholderAPI.setPlaceholders(player, line);
							line = line.replace("%name%", player.getName());
							line = line.replace("%dayOnlineTime%",
									OnlineTimes.turnToString(OnlineTimes.getDayTime(player)));
							line = line.replace("%totalOnlineTime%",
									OnlineTimes.turnToString(OnlineTimes.getTotalTime(player)));
							object.getScore(line).setScore(size--);
						}
						player.setScoreboard(board);
					}
				}
			}
		}.runTaskTimer(UntilTheEndServer.getInstance(), 1L, 20L);
	}
	
	@EventHandler public void onJoin(PlayerQuitEvent event) {
		disablers.remove(event.getPlayer().getUniqueId());
	}

	public static void changeState(Player player) {
		if (!player.hasPermission("utes.sb.toggle")) {
			player.sendMessage("您没有权限操控计分板！");
			return;
		}
		if (!disablers.contains(player.getUniqueId())) {
			disablers.add(player.getUniqueId());
			player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
			player.sendMessage("计分板已经关闭");
		} else {
			disablers.remove(player.getUniqueId());
			player.sendMessage("计分板已经开启");
		}
	}
}