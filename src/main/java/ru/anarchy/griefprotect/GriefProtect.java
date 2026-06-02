package ru.anarchy.griefprotect;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GriefProtect extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, String> lastRegion = new HashMap<>();
    private final String PREFIX = "§b§lFrostWorld §8» §7";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("gprotect").setExecutor(this);
        getLogger().info("[FrostWorld] Плагин мульти-приватов с командами запущен!");
    }

    @Override
    public void onDisable() {
        lastRegion.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("griefprotect.admin")) {
            sender.sendMessage(PREFIX + "§cУ вас нет прав на эту команду.");
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(PREFIX + "§cИгрок не найден на сервере.");
                return true;
            }

            String type = args[2].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(PREFIX + "§cУкажите правильное количество цифрой.");
                return true;
            }

            Material material;
            String itemName;

            switch (type) {
                case "iron":
                    material = Material.IRON_BLOCK;
                    itemName = "§7§l⚡ ЖЕЛЕЗНЫЙ КРИСТАЛЛ ПРИВАТА ⚡";
                    break;
                case "gold":
                    material = Material.GOLD_BLOCK;
                    itemName = "§6§l⚡ ЗОЛОТОЙ КРИСТАЛЛ ПРИВАТА ⚡";
                    break;
                case "diamond":
                    material = Material.DIAMOND_BLOCK;
                    itemName = "§b§l⚡ АЛМАЗНЫЙ КРИСТАЛЛ ПРИВАТА ⚡";
                    break;
                case "emerald":
                    material = Material.EMERALD_BLOCK;
                    itemName = "§a§l⚡ ИЗУМРУДНЫЙ КРИСТАЛЛ ПРИВАТА ⚡";
                    break;
                default:
                    sender.sendMessage(PREFIX + "§cТип блока не найден! Варианты: iron, gold, diamond, emerald");
                    return true;
            }

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                meta.setLore(Arrays.asList(
                        "§7Поставьте этот блок на землю,",
                        "§7чтобы заприватить территорию анархии."
                ));
                item.setItemMeta(meta);
            }

            target.getInventory().addItem(item);
            target.sendMessage(PREFIX + "Вам выдан блок привата: " + itemName);
            sender.sendMessage(PREFIX + "§aУспешно выдано " + amount + " шт. игроку " + target.getName());
            return true;
        }

        sender.sendMessage("§cИспользуйте: /gprotect give [ник] [iron/gold/diamond/emerald] [кол-во]");
        return true;
    }
}
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        Player player = event.getPlayer();
        Location loc = event.getTo();
        if (loc == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (regions == null) return;

        BlockVector3 position = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
        ApplicableRegionSet set = regions.getApplicableRegions(position);

        String currentRgId = "none";
        ProtectedRegion activeRegion = null;

        if (!set.getRegions().isEmpty()) {
            for (ProtectedRegion rg : set.getRegions()) {
                if (rg.getId().startsWith("ps_")) {
                    activeRegion = rg;
                    currentRgId = rg.getId();
                    break;
                }
            }
        }

        if (!lastRegion.getOrDefault(player.getUniqueId(), "none").equals(currentRgId)) {
            lastRegion.put(player.getUniqueId(), currentRgId);

            if (activeRegion != null) {
                String ownerName = activeRegion.getOwners().getPlayers().iterator().hasNext() ? 
                    Bukkit.getOfflinePlayer(activeRegion.getOwners().getPlayers().iterator().next()).getName() : "Неизвестен";

                player.sendTitle("§c§lЧУЖАЯ ТЕРРИТОРИЯ", "§7Владелец §8» §f" + ownerName, 10, 35, 10);
            } else {
                player.sendTitle("§e§lСВОБОДНАЯ ТЕРРИТОРИЯ", "§7Здесь можно строить базу", 10, 35, 10);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastRegion.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material type = block.getType();

        int radius = 0;
        String holoTitle = "";
        String chatBlockName = "";

        if (type == Material.IRON_BLOCK) {
            radius = 5;
            holoTitle = "§7§l⚡ ЖЕЛЕЗНЫЙ ПРИВАТ ⚡";
            chatBlockName = "§7железный§7";
        } else if (type == Material.GOLD_BLOCK) {
            radius = 8;
            holoTitle = "§6§l⚡ ЗОЛОТОЙ ПРИВАТ ⚡";
            chatBlockName = "§6золотой§7";
        } else if (type == Material.DIAMOND_BLOCK) {
            radius = 12;
            holoTitle = "§b§l⚡ АЛМАЗНЫЙ ПРИВАТ ⚡";
            chatBlockName = "§bалмазный§7";
        } else if (type == Material.EMERALD_BLOCK) {
            radius = 16;
            holoTitle = "§a§l⚡ ИЗУМРУДНЫЙ ПРИВАТ ⚡";
            chatBlockName = "§aизумрудный§7";
        } else {
            return; 
        }

        String id = block.getX() + "_" + block.getY() + "_" + block.getZ();
        String regionName = "ps_" + id;
        
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(block.getWorld()));
        if (regions == null) return;

        BlockVector3 min = BlockVector3.at(block.getX() - radius, 0, block.getZ() - radius);
        BlockVector3 max = BlockVector3.at(block.getX() + radius, 256, block.getZ() + radius);
        ProtectedRegion region = new ProtectedCuboidRegion(regionName, min, max);
        
        region.getOwners().addPlayer(player.getUniqueId());
        
        region.setFlag(Flags.TNT, StateFlag.State.ALLOW);
        region.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.ALLOW);
        region.setFlag(Flags.OTHER_EXPLOSION, StateFlag.State.ALLOW);
        regions.addRegion(region);

        Location holoLoc = block.getLocation().add(0.5, 1.4, 0.5); 
        String coordsText = "§7Координаты §8» §b" + block.getX() + "§7, §b" + block.getZ();
        
        DHAPI.createHologram("holo_" + id, holoLoc, Arrays.asList(
                holoTitle, 
                "§7Владелец §8» §f" + player.getName(),
                coordsText
        ));

        player.sendMessage(PREFIX + "Вы успешно установили " + chatBlockName + " блок привата! Радиус: §b" + radius + " блоков§7.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isProtectBlock(block.getType())) {
            removeProtection(block);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (isProtectBlock(block.getType())) {
                removeProtection(block);
            }
        }
    }

    private boolean isProtectBlock(Material material) {
        return material == Material.IRON_BLOCK || 
               material == Material.GOLD_BLOCK || 
               material == Material.DIAMOND_BLOCK || 
               material == Material.EMERALD_BLOCK;
    }

    private void removeProtection(Block block) {
        String id = block.getX() + "_" + block.getY() + "_" + block.getZ();
        
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(block.getWorld()));
        if (regions != null && regions.hasRegion("ps_" + id)) {
            regions.removeRegion("ps_" + id);
        }

        Hologram holo = DHAPI.getHologram("holo_" + id);
        if (holo != null) {
            holo.delete();
        }
    }
}