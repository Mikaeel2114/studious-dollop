package com.andrei1058.bedwars.arena.privatematch;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.gameplay.TeamAssignEvent;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.minestorm.MineStorm;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MineStorm private matches: an arena locked to a single party.
 * Created with /bw private. Only members of the leader's party can join,
 * party members are split across teams so they fight each other.
 */
public final class PrivateMatchManager implements Listener {

    public static final String PERMISSION_USE = "bw.private";
    public static final String PERMISSION_BYPASS = "bw.private.bypass";

    private static final Map<IArena, PrivateMatch> MATCHES = new HashMap<>();

    public static final class PrivateMatch {
        private final UUID leader;
        private final String leaderName;
        private final Set<UUID> allowed = new HashSet<>();

        private PrivateMatch(Player leader, Collection<Player> members) {
            this.leader = leader.getUniqueId();
            this.leaderName = leader.getName();
            allowed.add(leader.getUniqueId());
            for (Player m : members) allowed.add(m.getUniqueId());
        }

        public UUID getLeader() {
            return leader;
        }

        public String getLeaderName() {
            return leaderName;
        }

        public Set<UUID> getAllowed() {
            return allowed;
        }
    }

    // ------------------------------------------------------------------ API

    public static boolean isPrivate(IArena arena) {
        return arena != null && MATCHES.containsKey(arena);
    }

    public static PrivateMatch get(IArena arena) {
        return MATCHES.get(arena);
    }

    public static void lock(IArena arena, Player leader, Collection<Player> members) {
        MATCHES.put(arena, new PrivateMatch(leader, members));
        BedWars.debug("Private match locked: " + arena.getArenaName() + " by " + leader.getName());
    }

    public static void unlock(IArena arena) {
        if (MATCHES.remove(arena) != null) {
            BedWars.debug("Private match unlocked: " + arena.getArenaName());
        }
    }

    public static boolean canJoin(IArena arena, Player p) {
        PrivateMatch match = MATCHES.get(arena);
        if (match == null) return true;
        if (match.allowed.contains(p.getUniqueId())) return true;
        if (p.hasPermission(PERMISSION_BYPASS)) return true;
        // late party joiners
        Player leader = Bukkit.getPlayer(match.leader);
        if (leader != null && BedWars.getParty().isMember(leader, p)) {
            match.allowed.add(p.getUniqueId());
            return true;
        }
        return false;
    }

    /**
     * Spread every player across the arena teams (party members vs each other).
     */
    public static void assignTeams(IArena arena) {
        List<Player> players = new ArrayList<>(arena.getPlayers());
        Collections.shuffle(players);
        List<ITeam> teams = arena.getTeams();
        Collections.shuffle(teams);
        if (teams.isEmpty()) return;
        int used = Math.max(2, Math.min(teams.size(), players.size()));
        used = Math.min(used, teams.size());

        for (Player p : players) {
            ITeam target = null;
            for (int i = 0; i < used; i++) {
                ITeam t = teams.get(i);
                if (t.getMembers().size() >= arena.getMaxInTeam()) continue;
                if (target == null || t.getMembers().size() < target.getMembers().size()) target = t;
            }
            if (target == null) {
                // every used team is full, fall back to any team with room
                for (ITeam t : teams) {
                    if (t.getMembers().size() < arena.getMaxInTeam()) {
                        target = t;
                        break;
                    }
                }
            }
            if (target == null) target = teams.get(0);
            TeamAssignEvent e = new TeamAssignEvent(p, target, arena);
            Bukkit.getPluginManager().callEvent(e);
            if (!e.isCancelled()) {
                target.addPlayers(p);
            }
            p.closeInventory();
        }
    }

    // ------------------------------------------------------------------ listeners

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArenaJoin(PlayerJoinArenaEvent e) {
        if (e.isCancelled()) return;
        if (canJoin(e.getArena(), e.getPlayer())) return;
        e.setCancelled(true);
        PrivateMatch match = get(e.getArena());
        e.getPlayer().sendMessage(MineStorm.msg("&cThis is a &bprivate match &cof &b"
                + (match == null ? "a party" : match.getLeaderName()) + "&c. Only their party can join."));
    }

    @EventHandler
    public void onStateChange(GameStateChangeEvent e) {
        if (e.getNewState() == GameState.restarting) {
            unlock(e.getArena());
        }
    }

    @EventHandler
    public void onArenaLeave(PlayerLeaveArenaEvent e) {
        IArena arena = e.getArena();
        if (!isPrivate(arena)) return;
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            if (arena.getPlayers().isEmpty()
                    && (arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting)) {
                unlock(arena);
            }
        }, 2L);
    }
}
