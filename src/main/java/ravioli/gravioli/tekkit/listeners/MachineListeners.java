package ravioli.gravioli.tekkit.listeners;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.tasks.AutoEquipTask;
import java.lang.reflect.InvocationTargetException;

public class MachineListeners implements Listener {
    private Tekkit plugin;

    public MachineListeners(Tekkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        MachinesManager.loadMachinesInWorld(plugin, worldName);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        String worldName = event.getWorld().getName();

        int machinesSaved = 0;
        long start = System.currentTimeMillis();
        for (Machine machine : MachinesManager.getMachines()) {
            if (machine.getWorldName().equals(worldName)) {
                if (machine.isLoaded()) {
                    machine.save();
                    machinesSaved++;
                }
            }
        }
        if (machinesSaved > 0) {
            System.out.println("[Tekkit Inspired] Saved " + machinesSaved + " machines in '" + worldName + "' in " + (System.currentTimeMillis() - start) + "ms");
        }
        MachinesManager.deleteMachinesInWorld(plugin, worldName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        ItemStack item = event.getItemInHand();
        for (Machine machine : MachinesManager.registeredMachines) {
            if (machine.getRecipe().getResult().isSimilar(item)) {
                try {
                    Machine newMachine = machine.getClass().getConstructor(Tekkit.class).newInstance(plugin);
                    newMachine.create(event.getPlayer(), event.getBlock().getLocation());
                } catch (InstantiationException |
                         InvocationTargetException |
                         IllegalAccessException |
                         NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        if (Tekkit.AUTO_EQUIP) {
            Player player = event.getPlayer();
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && item.getAmount() == 1) {
                Bukkit.getScheduler().runTaskLater(plugin, new AutoEquipTask(player, item, event.getHand()), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        if (block.hasMetadata("machine")) {
            Object metadata = block.getMetadata("machine").get(0).value();
            if (metadata instanceof Machine) {
                ((Machine) metadata).onMachinePieceBreak(block);
            }
            block.removeMetadata("machine", plugin);
            block.setTypeIdAndData(0, (byte) 0, true);
        }
    }

    @EventHandler
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        if (Tekkit.AUTO_EQUIP) {
            Player player = event.getPlayer();
            ItemStack item = event.getBrokenItem();
            if (item != null) {
                EquipmentSlot slot = EquipmentSlot.HAND;
                if (player.getInventory().getItemInOffHand().equals(item)) {
                    slot = EquipmentSlot.OFF_HAND;
                }
                Bukkit.getScheduler().runTaskLater(plugin, new AutoEquipTask(player, item, slot), 1);
            }
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (event.getItem().hasMetadata("pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        if (MachinesManager.isMachine(block.getLocation())) {
            Machine machine = MachinesManager.getMachineByLocation(block.getLocation());
            machine.destroy(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (Block block : event.blockList()) {
            if (MachinesManager.isMachine(block.getLocation())) {
                Machine machine = MachinesManager.getMachineByLocation(block.getLocation());
                machine.destroy(true);
            }
        }
    }
}