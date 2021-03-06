package com.github.games647.scoreboardstats.pvpstats;

import com.github.games647.scoreboardstats.ScoreboardStats;

import java.lang.ref.WeakReference;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This class is used for loading the player stats.
 */
public class StatsLoader implements Runnable {

    protected final ScoreboardStats plugin;

    private final boolean uuidSearch;
    private final WeakReference<Player> weakPlayer;
    private final WeakReference<Database> weakDatabase;

    /**
     * Creates a new loader for a specific player
     *
     * @param plugin the owning plugin to reschedule
     * @param uuidSearch should it be searched by uuid
     * @param player player instance
     * @param statsDatabase the pvp database
     */
    public StatsLoader(ScoreboardStats plugin, boolean uuidSearch, Player player, Database statsDatabase) {
        this.plugin = plugin;
        this.uuidSearch = uuidSearch;

        //don't prevent the garbage collection of this player if he logs out
        this.weakPlayer = new WeakReference<Player>(player);
        this.weakDatabase = new WeakReference<Database>(statsDatabase);
    }

    @Override
    public void run() {
        final Player player = weakPlayer.get();
        final Database statsDatabase = weakDatabase.get();
        if (player != null && statsDatabase != null) {
            final PlayerStats stats = statsDatabase.loadAccount(player);
            //update player name on every load, because it's changeable
            stats.setPlayername(player.getName());
            if (uuidSearch) {
                //Set the uuid if the server is uuid compatible
                stats.setUuid(player.getUniqueId());
            }

            new BukkitRunnable() {

                @Override
                public void run() {
                    //possible not thread-safe, so reschedule it while setMetadata is thread-safe
                    if (player.isOnline()) {
                        //sets it only if the player is only
                        player.setMetadata("player_stats", new FixedMetadataValue(plugin, stats));
                        plugin.getReplaceManager().updateScore(player, "deaths", stats.getDeaths());
                        plugin.getReplaceManager().updateScore(player, "kdr", stats.getKdr());
                        plugin.getReplaceManager().updateScore(player, "kills", stats.getKills());
                        plugin.getReplaceManager().updateScore(player, "killstreak", stats.getKillstreak());
                        plugin.getReplaceManager().updateScore(player, "current_streak", stats.getLaststreak());
                        plugin.getReplaceManager().updateScore(player, "mobkills", stats.getMobkills());
                    }
                }
            }.runTask(plugin);
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
