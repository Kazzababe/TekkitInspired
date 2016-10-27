package ravioli.gravioli.tekkit.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.schedulers.AutoEquipTask;

public class MachineListeners implements Listener {
    private Tekkit plugin;

    public MachineListeners(Tekkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        for (MachineBase machine: MachineManager.getRegisteredMachines()) {
            if (machine.getRecipe().getResult().isSimilar(item)) {
                try {
                    MachineBase newMachine = machine.getClass().newInstance();
                    newMachine.create(event.getPlayer(), event.getBlock().getLocation());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }


        if (Tekkit.AUTO_EQUIP) {
            Player player = event.getPlayer();
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && item.getAmount() == 1) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new AutoEquipTask(player, item, event.getHand()), 1);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.hasMetadata("machine")) {
            Object metadata = block.getMetadata("machine").get(0).value();
            if (metadata instanceof MachineBase) {
                ((MachineBase) metadata).onMachinePieceBreak(block);
            }
            block.removeMetadata("machine", this.plugin);
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
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new AutoEquipTask(player, item, slot), 1);
            }
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (event.getItem().hasMetadata("pickup")) {
            event.setCancelled(true);
        }
    }
}