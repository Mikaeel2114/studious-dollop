package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.privatematch.PrivateMatchManager;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.minestorm.MineStorm;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * MineStorm: /bw private [group|arena]
 * Starts a private BedWars match for your party only (party from BedFight /party).
 */
public class CmdPrivate extends SubCommand {

    public CmdPrivate(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(14);
        showInList(true);
        setDisplayInfo(MainCommand.createTC("§b ▪ §7/" + MainCommand.getInstance().getName() + " " + getSubCommandName() + " §8[group] §8- §bprivate match with your party",
                "/" + getParent().getName() + " " + getSubCommandName(),
                "§fStart a §bprivate match §fwith your party only.\n§fYou must be the party leader.\n§7Usage: /" + getParent().getName() + " " + getSubCommandName() + " [group|arena]"));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) {
            s.sendMessage(MineStorm.msg("&cOnly players can use this command."));
            return true;
        }
        Player p = (Player) s;

        if (!p.hasPermission(PrivateMatchManager.PERMISSION_USE)) {
            p.sendMessage(MineStorm.msg("&cYou don't have permission to start private matches."));
            return true;
        }
        if (SetupSession.isInSetupSession(p.getUniqueId())) {
            p.sendMessage(MineStorm.msg("&cYou can't do that during an arena setup."));
            return true;
        }
        if (Arena.getArenaByPlayer(p) != null) {
            p.sendMessage(MineStorm.msg("&cLeave your current game first with &b/" + getParent().getName() + " leave&c."));
            return true;
        }
        if (!BedWars.getParty().hasParty(p)) {
            p.sendMessage(MineStorm.msg("&cYou need a party to play a private match. &7Use &b/party invite <player>&7."));
            return true;
        }
        if (!BedWars.getParty().isOwner(p)) {
            p.sendMessage(MineStorm.msg("&cOnly the &bparty leader &ccan start a private match."));
            return true;
        }

        List<Player> members = new ArrayList<>();
        for (Player m : BedWars.getParty().getMembers(p)) {
            if (m == null || !m.isOnline()) continue;
            IArena in = Arena.getArenaByPlayer(m);
            if (in != null && !in.isSpectator(m)) continue; // busy in another game
            members.add(m);
        }
        if (!members.contains(p)) members.add(p);
        if (members.size() < 2) {
            p.sendMessage(MineStorm.msg("&cYou need at least &b2 &conline party members who are not in a game."));
            return true;
        }

        String filter = args.length > 0 ? args[0] : null;
        IArena arena = findArena(members.size(), filter);
        if (arena == null) {
            p.sendMessage(MineStorm.msg("&cNo free arena found for &b" + members.size() + " &cplayers"
                    + (filter == null ? "" : " &cin &b" + filter) + "&c. Try again in a moment."));
            return true;
        }

        PrivateMatchManager.lock(arena, p, members);
        if (!arena.addPlayer(p, false)) {
            PrivateMatchManager.unlock(arena);
            p.sendMessage(MineStorm.msg("&cCould not start the private match, please try again."));
            return true;
        }

        for (Player m : members) {
            m.sendMessage(MineStorm.msg("&7Private match on &b" + arena.getDisplayName() + " &7started by &b" + p.getName() + "&7!"));
        }

        // Party-only arenas never reach the normal auto-start rule (one party = one team),
        // so start the countdown ourselves once everybody is in.
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            if (!PrivateMatchManager.isPrivate(arena)) return;
            if (arena.getStatus() != GameState.waiting) return;
            if (arena.getPlayers().size() < 2) return;
            arena.changeStatus(GameState.starting);
        }, 20L);
        return true;
    }

    private IArena findArena(int size, String filter) {
        IArena best = null;
        for (IArena a : new ArrayList<>(Arena.getArenas())) {
            if (a.getStatus() != GameState.waiting) continue;
            if (!a.getPlayers().isEmpty()) continue;
            if (PrivateMatchManager.isPrivate(a)) continue;
            if (a.getTeams().size() < 2) continue;
            if (a.getMaxPlayers() < size) continue;
            if (filter != null && !(filter.equalsIgnoreCase(a.getGroup())
                    || filter.equalsIgnoreCase(a.getArenaName())
                    || filter.equalsIgnoreCase(a.getDisplayName()))) continue;
            if (best == null || a.getMaxPlayers() < best.getMaxPlayers()) best = a;
        }
        return best;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> groups = new ArrayList<>();
        for (IArena a : Arena.getArenas()) {
            if (a.getGroup() != null && !groups.contains(a.getGroup())) groups.add(a.getGroup());
        }
        return groups;
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (Arena.getArenaByPlayer(p) != null) return false;
        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return p.hasPermission(PrivateMatchManager.PERMISSION_USE);
    }
}
