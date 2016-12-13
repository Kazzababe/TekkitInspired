package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Diode;
import org.bukkit.scheduler.BukkitRunnable;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.Machine;

import java.util.ArrayList;
import java.util.List;

public class MachineTimer extends Machine {
    private int delay = 1;
    private int facing;

    public MachineTimer(Tekkit plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        this.updateTask(5 * delay);

        Diode diode = (Diode) getBlock().getState().getData();
        BlockFace facing = diode.getFacing();
        this.facing = (facing == BlockFace.SOUTH ? 2 : facing == BlockFace.EAST ? 1 : facing == BlockFace.NORTH ? 0 : 3);
        delay = diode.getDelay();
    }

    @Override
    public void runMachine() {
        facing = facing == 3? 0 : facing + 1;
        this.getBlock().setTypeIdAndData(Material.DIODE_BLOCK_ON.getId(), (byte) (facing + 4 * (delay - 1)), true);
        new BukkitRunnable() {
            public void run() {
                if (getBlock().getType() == Material.DIODE_BLOCK_ON) {
                    getBlock().setTypeIdAndData(Material.DIODE_BLOCK_OFF.getId(), (byte) (facing + 4 * (delay - 1)), true);
                }
            }
        }.runTaskLater(getPlugin(), 3);
    }

    @Override
    public List<ItemStack> getDrops() {
        return new ArrayList();
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.DIODE);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Redstone Timer");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("CRC", "RTR", "CRC");
        recipe.setIngredient('C', Material.STONE);
        recipe.setIngredient('T', Material.REDSTONE_COMPARATOR);
        recipe.setIngredient('R', Material.REDSTONE);

        return recipe;
    }

    @Override
    public String getTableName() {
        return "RedstoneTimer";
    }

    @Override
    public String getName() {
        return "timer";
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                !event.getPlayer().isSneaking() &&
                event.getClickedBlock().getLocation().equals(this.getLocation())) {
            event.setCancelled(true);
            delay = (delay == 4 ? 1 : delay + 1);
            getBlock().setData((byte) (4 * (delay - 1) + facing));
            updateTask(5 * delay);
        }
    }

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (event.getBlock().getLocation().equals(getLocation())) {
            event.setNewCurrent(1);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getToBlock().getLocation().equals(getLocation())) {
            destroy(true);
        }
    }
}
