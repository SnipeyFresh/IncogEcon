package com.snipeyfresh.incogshop.trade;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.economy.WalletManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeManager {
    public static final int OFFER_SLOTS = 9;

    public record Result(boolean success, String message) {}

    private record TradeRequest(UUID requester, UUID target, long createdAt, long expiresAt) {}

    public static final class TradeSession {
        private final UUID id = UUID.randomUUID();
        private final UUID left;
        private final UUID right;
        private final String leftName;
        private final String rightName;
        private final ItemStack[] leftItems = new ItemStack[OFFER_SLOTS];
        private final ItemStack[] rightItems = new ItemStack[OFFER_SLOTS];
        private double leftMoney;
        private double rightMoney;
        private boolean leftConfirmed;
        private boolean rightConfirmed;

        private TradeSession(Player left, Player right) {
            this.left = left.getUniqueId();
            this.right = right.getUniqueId();
            this.leftName = left.getName();
            this.rightName = right.getName();
        }

        public UUID id() { return id; }
        public UUID left() { return left; }
        public UUID right() { return right; }
        public String leftName() { return leftName; }
        public String rightName() { return rightName; }
        public boolean isParticipant(UUID player) { return left.equals(player) || right.equals(player); }
        public boolean isLeft(UUID player) { return left.equals(player); }
        public UUID other(UUID player) { return left.equals(player) ? right : left; }
        public String otherName(UUID player) { return left.equals(player) ? rightName : leftName; }
        public double money(UUID player) { return left.equals(player) ? leftMoney : rightMoney; }
        public boolean confirmed(UUID player) { return left.equals(player) ? leftConfirmed : rightConfirmed; }

        public ItemStack[] items(UUID player) {
            ItemStack[] source = left.equals(player) ? leftItems : rightItems;
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
            return copy;
        }

        public int itemCount(UUID player) {
            ItemStack[] source = left.equals(player) ? leftItems : rightItems;
            int total = 0;
            for (ItemStack stack : source) if (stack != null && !stack.getType().isAir()) total += stack.getAmount();
            return total;
        }
    }

    private final IncogShopPlugin plugin;
    private final Map<UUID, LinkedHashMap<UUID, TradeRequest>> incoming = new HashMap<>();
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> sessionByPlayer = new HashMap<>();

    public TradeManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("trading.enabled", true);
    }

    public TradeSession session(UUID sessionId) {
        return sessions.get(sessionId);
    }

    public TradeSession sessionFor(UUID playerId) {
        UUID id = sessionByPlayer.get(playerId);
        return id == null ? null : sessions.get(id);
    }

    public boolean busy(UUID playerId) {
        return sessionByPlayer.containsKey(playerId);
    }

    public Result request(Player requester, Player target) {
        if (!enabled()) return new Result(false, "Trading is currently disabled.");
        if (requester.getUniqueId().equals(target.getUniqueId())) return new Result(false, "You cannot trade with yourself.");
        if (busy(requester.getUniqueId())) return new Result(false, "You are already in a trade.");
        if (busy(target.getUniqueId())) return new Result(false, target.getName() + " is already in a trade.");

        long timeoutMs = Math.max(5, plugin.getConfig().getInt("trading.request-timeout-seconds", 30)) * 1000L;
        long now = System.currentTimeMillis();
        incoming.computeIfAbsent(target.getUniqueId(), ignored -> new LinkedHashMap<>())
                .put(requester.getUniqueId(), new TradeRequest(requester.getUniqueId(), target.getUniqueId(), now, now + timeoutMs));

        target.sendMessage(plugin.prefix() + "§e" + requester.getName() + " §fwants to trade with you.");
        target.sendMessage(plugin.prefix() + "§7Use §f/trade accept " + requester.getName() + " §7or §f/trade deny " + requester.getName() + "§7.");
        return new Result(true, "Trade request sent to " + target.getName() + ".");
    }

    public Result accept(Player target, String requesterName) {
        if (!enabled()) return new Result(false, "Trading is currently disabled.");
        if (busy(target.getUniqueId())) return new Result(false, "You are already in a trade.");

        TradeRequest request = findIncoming(target.getUniqueId(), requesterName);
        if (request == null) return new Result(false, requesterName == null ? "You have no pending trade requests." : "No pending trade request from " + requesterName + ".");
        removeRequest(request);

        if (request.expiresAt() < System.currentTimeMillis()) return new Result(false, "That trade request has expired.");
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) return new Result(false, "That player is no longer online.");
        if (busy(requester.getUniqueId())) return new Result(false, requester.getName() + " is already in another trade.");

        clearRequestsFor(requester.getUniqueId());
        clearRequestsFor(target.getUniqueId());

        TradeSession session = new TradeSession(requester, target);
        sessions.put(session.id(), session);
        sessionByPlayer.put(requester.getUniqueId(), session.id());
        sessionByPlayer.put(target.getUniqueId(), session.id());

        requester.sendMessage(plugin.prefix() + "§aTrade started with §f" + target.getName() + "§a.");
        target.sendMessage(plugin.prefix() + "§aTrade started with §f" + requester.getName() + "§a.");
        plugin.tradeGui().open(requester, session);
        plugin.tradeGui().open(target, session);
        return new Result(true, "Accepted " + requester.getName() + "'s trade request.");
    }

    public Result deny(Player target, String requesterName) {
        TradeRequest request = findIncoming(target.getUniqueId(), requesterName);
        if (request == null) return new Result(false, requesterName == null ? "You have no pending trade requests." : "No pending trade request from " + requesterName + ".");
        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester != null) requester.sendMessage(plugin.prefix() + "§c" + target.getName() + " declined your trade request.");
        return new Result(true, "Trade request declined.");
    }

    public Result cancel(Player player) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null) return new Result(false, "You are not currently in a trade.");
        cancelSession(session, "Trade cancelled.", player.getUniqueId());
        return new Result(true, "Trade cancelled.");
    }

    public void markOfferEditing(Player player) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null) return;
        resetConfirmations(session);
        refresh(session);
    }

    public boolean addItem(Player player, ItemStack stack) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null || stack == null || stack.getType().isAir() || stack.getAmount() <= 0) return false;
        ItemStack[] offer = offerArray(session, player.getUniqueId());
        for (int i = 0; i < offer.length; i++) {
            if (offer[i] == null || offer[i].getType().isAir()) {
                offer[i] = stack.clone();
                resetConfirmations(session);
                refresh(session);
                return true;
            }
        }
        return false;
    }

    public boolean removeItem(Player player, int offerIndex) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null || offerIndex < 0 || offerIndex >= OFFER_SLOTS) return false;
        ItemStack[] offer = offerArray(session, player.getUniqueId());
        ItemStack stack = offer[offerIndex];
        if (stack == null || stack.getType().isAir()) return false;
        offer[offerIndex] = null;
        resetConfirmations(session);
        plugin.stash().deliverOrStash(player, stack.clone());
        refresh(session);
        return true;
    }

    public Result setMoney(Player player, double amount) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null) return new Result(false, "That trade is no longer active.");
        amount = WalletManager.round(Math.max(0, amount));
        double max = Math.max(0, plugin.getConfig().getDouble("trading.maximum-money-offer", 1_000_000_000_000.0));
        if (!Double.isFinite(amount) || amount > max) return new Result(false, "That money offer is too large.");
        if (plugin.wallets().get(player.getUniqueId()) + 1e-9 < amount) return new Result(false, "You do not have enough money for that offer.");

        if (session.isLeft(player.getUniqueId())) session.leftMoney = amount;
        else session.rightMoney = amount;
        resetConfirmations(session);
        refresh(session);
        return new Result(true, "Money offer set to " + plugin.money(amount) + ".");
    }

    public Result confirm(Player player) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session == null) return new Result(false, "That trade is no longer active.");
        if (plugin.wallets().get(player.getUniqueId()) + 1e-9 < session.money(player.getUniqueId())) {
            resetConfirmations(session);
            refresh(session);
            return new Result(false, "Your balance is below your offered amount. Change your money offer and try again.");
        }

        if (session.isLeft(player.getUniqueId())) session.leftConfirmed = true;
        else session.rightConfirmed = true;
        refresh(session);

        Player other = Bukkit.getPlayer(session.other(player.getUniqueId()));
        if (other != null && other.isOnline()) other.sendMessage(plugin.prefix() + "§e" + player.getName() + " §7confirmed the trade.");

        if (session.leftConfirmed && session.rightConfirmed) {
            return complete(session);
        }
        return new Result(true, "Trade confirmed. Waiting for " + session.otherName(player.getUniqueId()) + ".");
    }

    private Result complete(TradeSession session) {
        Player left = Bukkit.getPlayer(session.left);
        Player right = Bukkit.getPlayer(session.right);
        if (left == null || right == null || !left.isOnline() || !right.isOnline()) {
            cancelSession(session, "Trade cancelled because a player disconnected.", null);
            return new Result(false, "Trade cancelled because a player disconnected.");
        }

        if (plugin.wallets().get(session.left) + 1e-9 < session.leftMoney) {
            resetConfirmations(session);
            refresh(session);
            return new Result(false, left.getName() + " no longer has enough money for the offered amount.");
        }
        if (plugin.wallets().get(session.right) + 1e-9 < session.rightMoney) {
            resetConfirmations(session);
            refresh(session);
            return new Result(false, right.getName() + " no longer has enough money for the offered amount.");
        }

        if (!settleMoney(session)) {
            resetConfirmations(session);
            refresh(session);
            return new Result(false, "The economy provider rejected the money transfer. No items were exchanged.");
        }

        removeSession(session);

        int leftOverflow = deliverItems(right, session.leftItems);
        int rightOverflow = deliverItems(left, session.rightItems);

        closeTradeInventory(left, session.id());
        closeTradeInventory(right, session.id());

        String leftItems = summarize(session.leftItems);
        String rightItems = summarize(session.rightItems);
        plugin.market().audit("PLAYER_TRADE", session.left, session.leftName, session.right.toString(), session.itemCount(session.left), session.leftMoney,
                "partner=" + session.rightName + ";receivedMoney=" + WalletManager.round(session.rightMoney) + ";gave=" + leftItems + ";received=" + rightItems);
        plugin.market().audit("PLAYER_TRADE", session.right, session.rightName, session.left.toString(), session.itemCount(session.right), session.rightMoney,
                "partner=" + session.leftName + ";receivedMoney=" + WalletManager.round(session.leftMoney) + ";gave=" + rightItems + ";received=" + leftItems);

        left.sendMessage(plugin.prefix() + "§aTrade completed with §f" + right.getName() + "§a."
                + (rightOverflow > 0 ? " §d" + rightOverflow + " received item(s) went to /stash." : ""));
        right.sendMessage(plugin.prefix() + "§aTrade completed with §f" + left.getName() + "§a."
                + (leftOverflow > 0 ? " §d" + leftOverflow + " received item(s) went to /stash." : ""));
        return new Result(true, "Trade completed.");
    }

    private boolean settleMoney(TradeSession session) {
        double net = WalletManager.round(session.leftMoney - session.rightMoney);
        if (Math.abs(net) < 0.005) return true;

        UUID payer = net > 0 ? session.left : session.right;
        UUID receiver = net > 0 ? session.right : session.left;
        double amount = Math.abs(net);
        if (!plugin.wallets().withdraw(payer, amount)) return false;
        if (plugin.wallets().deposit(receiver, amount)) return true;

        if (!plugin.wallets().deposit(payer, amount)) {
            plugin.getLogger().severe("Failed to refund " + amount + " after a rejected trade deposit for " + payer + ".");
        }
        return false;
    }

    private int deliverItems(Player recipient, ItemStack[] items) {
        int overflow = 0;
        for (ItemStack stack : items) {
            if (stack == null || stack.getType().isAir()) continue;
            overflow += plugin.stash().deliverOrStash(recipient, stack.clone());
        }
        return overflow;
    }

    public void cancelForQuit(Player player) {
        TradeSession session = sessionFor(player.getUniqueId());
        if (session != null) cancelSession(session, "Trade cancelled because a player disconnected.", player.getUniqueId());
        clearRequestsFor(player.getUniqueId());
    }

    public void expireRequests() {
        long now = System.currentTimeMillis();
        List<TradeRequest> expired = new ArrayList<>();
        for (var targetEntry : incoming.entrySet()) {
            for (TradeRequest request : targetEntry.getValue().values()) if (request.expiresAt() <= now) expired.add(request);
        }
        for (TradeRequest request : expired) {
            removeRequest(request);
            Player requester = Bukkit.getPlayer(request.requester());
            if (requester != null) requester.sendMessage(plugin.prefix() + "§7Your trade request expired.");
        }
    }

    public void shutdown() {
        for (TradeSession session : new ArrayList<>(sessions.values())) {
            cancelSession(session, "Trade cancelled because IncogEcon is reloading or shutting down.", null);
        }
        incoming.clear();
    }

    private void cancelSession(TradeSession session, String message, UUID actor) {
        if (session == null || !sessions.containsKey(session.id())) return;
        removeSession(session);
        returnItems(session.left, session.leftItems);
        returnItems(session.right, session.rightItems);

        Player left = Bukkit.getPlayer(session.left);
        Player right = Bukkit.getPlayer(session.right);
        if (left != null) {
            closeTradeInventory(left, session.id());
            left.sendMessage(plugin.prefix() + "§c" + message);
        }
        if (right != null) {
            closeTradeInventory(right, session.id());
            right.sendMessage(plugin.prefix() + "§c" + message);
        }
        plugin.market().audit("PLAYER_TRADE_CANCEL", actor, actor == null ? "SYSTEM" : Bukkit.getOfflinePlayer(actor).getName(), session.id().toString(), 0, 0,
                "left=" + session.leftName + ";right=" + session.rightName);
    }

    private void returnItems(UUID owner, ItemStack[] items) {
        Player player = Bukkit.getPlayer(owner);
        for (ItemStack stack : items) {
            if (stack == null || stack.getType().isAir()) continue;
            if (player != null && player.isOnline()) plugin.stash().deliverOrStash(player, stack.clone());
            else plugin.stash().add(owner, stack.clone());
        }
    }

    private void removeSession(TradeSession session) {
        sessions.remove(session.id());
        sessionByPlayer.remove(session.left);
        sessionByPlayer.remove(session.right);
    }

    private void closeTradeInventory(Player player, UUID sessionId) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof com.snipeyfresh.incogshop.gui.TradeGui.Holder holder
                && holder.sessionId().equals(sessionId)) {
            player.closeInventory();
        }
    }

    private ItemStack[] offerArray(TradeSession session, UUID player) {
        return session.isLeft(player) ? session.leftItems : session.rightItems;
    }

    private void resetConfirmations(TradeSession session) {
        session.leftConfirmed = false;
        session.rightConfirmed = false;
    }

    private void refresh(TradeSession session) {
        plugin.tradeGui().refresh(session);
    }

    private TradeRequest findIncoming(UUID target, String requesterName) {
        LinkedHashMap<UUID, TradeRequest> requests = incoming.get(target);
        if (requests == null || requests.isEmpty()) return null;
        long now = System.currentTimeMillis();

        if (requesterName != null && !requesterName.isBlank()) {
            for (TradeRequest request : requests.values()) {
                if (request.expiresAt() <= now) continue;
                Player requester = Bukkit.getPlayer(request.requester());
                String name = requester != null ? requester.getName() : Bukkit.getOfflinePlayer(request.requester()).getName();
                if (name != null && name.equalsIgnoreCase(requesterName)) return request;
            }
            return null;
        }

        TradeRequest newest = null;
        for (TradeRequest request : requests.values()) {
            if (request.expiresAt() <= now) continue;
            if (newest == null || request.createdAt() > newest.createdAt()) newest = request;
        }
        return newest;
    }

    private void removeRequest(TradeRequest request) {
        LinkedHashMap<UUID, TradeRequest> requests = incoming.get(request.target());
        if (requests == null) return;
        requests.remove(request.requester());
        if (requests.isEmpty()) incoming.remove(request.target());
    }

    private void clearRequestsFor(UUID player) {
        incoming.remove(player);
        Iterator<Map.Entry<UUID, LinkedHashMap<UUID, TradeRequest>>> targets = incoming.entrySet().iterator();
        while (targets.hasNext()) {
            var entry = targets.next();
            entry.getValue().remove(player);
            if (entry.getValue().isEmpty()) targets.remove();
        }
    }

    private String summarize(ItemStack[] items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.getType().isAir()) continue;
            counts.merge(stack.getType().name(), stack.getAmount(), Integer::sum);
        }
        if (counts.isEmpty()) return "none";
        StringBuilder out = new StringBuilder();
        for (var entry : counts.entrySet()) {
            if (!out.isEmpty()) out.append(',');
            out.append(entry.getKey()).append('x').append(entry.getValue());
        }
        return out.toString();
    }
}
