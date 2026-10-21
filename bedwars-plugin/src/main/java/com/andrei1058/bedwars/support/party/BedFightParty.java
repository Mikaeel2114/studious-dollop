package com.andrei1058.bedwars.support.party;

import com.andrei1058.bedwars.api.party.Party;
import com.andrei1058.bedwars.minestorm.MineStorm;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MineStorm: bridge between BedWars and the BedFight party system (/party).
 * <p>
 * BedFight is accessed by reflection so this plugin compiles and runs
 * without BedFight on the class path. When BedFight is installed, every
 * BedWars party feature (party join, team-up, /bw private) uses BedFight parties.
 */
public class BedFightParty implements Party {

    private static final String PKG = "com.example.bedfight.party.";

    private final Object manager;
    private final Method mOf, mLeave, mDisband, mKick, mTransfer;
    private final Method pGetLeader, pIds, pHas, pSize, pGetMembers;
    private Method mDefaultLimit;
    private Field byPlayerField;
    private Constructor<?> partyConstructor;
    private Object memberRole;

    /**
     * @return the BedFight plugin instance if installed and enabled.
     */
    public static Plugin findBedFight() {
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            if (!pl.isEnabled()) continue;
            if (pl.getName().equalsIgnoreCase("BedFight")
                    || pl.getClass().getName().equals("com.example.bedfight.BedFightPlugin")) {
                return pl;
            }
        }
        return null;
    }

    public static boolean isBedFightPresent() {
        return findBedFight() != null;
    }

    /**
     * Try to hook into BedFight.
     *
     * @return adapter or null if BedFight is missing or incompatible.
     */
    public static BedFightParty tryHook() {
        Plugin pl = findBedFight();
        if (pl == null) return null;
        try {
            return new BedFightParty(pl);
        } catch (Throwable t) {
            MineStorm.console("&cCould not hook into BedFight party system: " + t);
            return null;
        }
    }

    private BedFightParty(Plugin bedFight) throws Exception {
        ClassLoader cl = bedFight.getClass().getClassLoader();
        Class<?> managerClass = Class.forName(PKG + "PartyManager", true, cl);
        Class<?> partyClass = Class.forName(PKG + "Party", true, cl);

        this.manager = locateManager(bedFight, managerClass);
        if (this.manager == null) {
            throw new IllegalStateException("PartyManager instance not found inside BedFight");
        }

        mOf = managerClass.getMethod("of", Player.class);
        mLeave = managerClass.getMethod("leave", Player.class);
        mDisband = managerClass.getMethod("disband", Player.class);
        mKick = managerClass.getMethod("kick", Player.class, Player.class, String.class);
        mTransfer = managerClass.getMethod("transfer", Player.class, Player.class, String.class);

        pGetLeader = partyClass.getMethod("getLeader");
        pIds = partyClass.getMethod("ids");
        pHas = partyClass.getMethod("has", UUID.class);
        pSize = partyClass.getMethod("size");
        pGetMembers = partyClass.getMethod("getMembers");

        // optional internals, used only for createParty/addMember
        try {
            mDefaultLimit = managerClass.getMethod("defaultLimit");
        } catch (NoSuchMethodException ignored) {
        }
        try {
            byPlayerField = managerClass.getDeclaredField("byPlayer");
            byPlayerField.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            partyConstructor = partyClass.getDeclaredConstructor(UUID.class, int.class, boolean.class, boolean.class);
            partyConstructor.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            Class<?> roleClass = Class.forName(PKG + "Party$Role", true, cl);
            for (Object c : roleClass.getEnumConstants()) {
                if (c.toString().equals("MEMBER")) memberRole = c;
            }
        } catch (Exception ignored) {
        }
    }

    private static Object locateManager(Object plugin, Class<?> managerClass) {
        // 1) public getters (getPartyManager(), getParties(), ...)
        for (Method m : plugin.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && managerClass.isAssignableFrom(m.getReturnType())) {
                try {
                    Object o = m.invoke(Modifier.isStatic(m.getModifiers()) ? null : plugin);
                    if (o != null) return o;
                } catch (Exception ignored) {
                }
            }
        }
        // 2) fields (any visibility) through the class hierarchy
        Class<?> c = plugin.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (managerClass.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object o = f.get(Modifier.isStatic(f.getModifiers()) ? null : plugin);
                        if (o != null) return o;
                    } catch (Exception ignored) {
                    }
                }
            }
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && managerClass.isAssignableFrom(m.getReturnType())) {
                    try {
                        m.setAccessible(true);
                        Object o = m.invoke(Modifier.isStatic(m.getModifiers()) ? null : plugin);
                        if (o != null) return o;
                    } catch (Exception ignored) {
                    }
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    // ---------------------------------------------------------------- helpers

    private Object partyOf(Player p) {
        if (p == null) return null;
        try {
            return mOf.invoke(manager, p);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID leaderOf(Object party) {
        try {
            return (UUID) pGetLeader.invoke(party);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<UUID> idsOf(Object party) {
        try {
            return new ArrayList<>((Collection<UUID>) pIds.invoke(party));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ---------------------------------------------------------------- Party API

    @Override
    public boolean hasParty(Player p) {
        return partyOf(p) != null;
    }

    @Override
    public int partySize(Player p) {
        Object party = partyOf(p);
        if (party == null) return 0;
        try {
            return (int) pSize.invoke(party);
        } catch (Exception e) {
            return idsOf(party).size();
        }
    }

    @Override
    public boolean isOwner(Player p) {
        Object party = partyOf(p);
        if (party == null) return false;
        return p.getUniqueId().equals(leaderOf(party));
    }

    @Override
    public List<Player> getMembers(Player owner) {
        List<Player> list = new ArrayList<>();
        Object party = partyOf(owner);
        if (party == null) return list;
        for (UUID id : idsOf(party)) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.isOnline()) list.add(m);
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createParty(Player owner, Player... members) {
        if (!hasParty(owner)) {
            if (partyConstructor == null || byPlayerField == null) return;
            try {
                int limit = mDefaultLimit == null ? 8 : (int) mDefaultLimit.invoke(manager);
                limit = Math.max(limit, members.length + 1);
                Object party = partyConstructor.newInstance(owner.getUniqueId(), limit, false, false);
                ((Map<Object, Object>) byPlayerField.get(manager)).put(owner.getUniqueId(), party);
            } catch (Exception e) {
                MineStorm.console("&cCould not create BedFight party: " + e);
                return;
            }
        }
        for (Player m : members) {
            addMember(owner, m);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addMember(Player owner, Player member) {
        if (member == null || owner == null || member.equals(owner)) return;
        Object party = partyOf(owner);
        if (party == null) {
            createParty(owner, member);
            return;
        }
        if (hasParty(member) || memberRole == null || byPlayerField == null) return;
        try {
            ((Map<Object, Object>) pGetMembers.invoke(party)).put(member.getUniqueId(), memberRole);
            ((Map<Object, Object>) byPlayerField.get(manager)).put(member.getUniqueId(), party);
        } catch (Exception e) {
            MineStorm.console("&cCould not add member to BedFight party: " + e);
        }
    }

    @Override
    public void removeFromParty(Player member) {
        try {
            mLeave.invoke(manager, member);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void disband(Player owner) {
        try {
            mDisband.invoke(manager, owner);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isMember(Player owner, Player check) {
        Object party = partyOf(owner);
        if (party == null || check == null) return false;
        try {
            return (boolean) pHas.invoke(party, check.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void removePlayer(Player owner, Player target) {
        try {
            mKick.invoke(manager, owner, target, target.getName());
        } catch (Exception ignored) {
        }
    }

    @Override
    public Player getOwner(Player member) {
        Object party = partyOf(member);
        if (party == null) return null;
        UUID leader = leaderOf(party);
        return leader == null ? null : Bukkit.getPlayer(leader);
    }

    @Override
    public void promote(@NotNull Player owner, @NotNull Player target) {
        try {
            mTransfer.invoke(manager, owner, target, target.getName());
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isInternal() {
        return false;
    }
}
