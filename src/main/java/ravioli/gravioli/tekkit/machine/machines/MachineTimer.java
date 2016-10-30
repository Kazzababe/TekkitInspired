package ravioli.gravioli.tekkit.machine.machines;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Diode;
import org.bukkit.scheduler.BukkitRunnable;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;

public class MachineTimer extends MachineBase {
    private int delay = 1;
    private int facing;

    @Override
    public void run() {
        this.facing = this.facing == 3? 0 : this.facing + 1;
        this.getBlock().setTypeIdAndData(Material.DIODE_BLOCK_ON.getId(), (byte) (this.facing + 4 * (this.delay - 1)), true);
        new BukkitRunnable() {
            public void run() {
                if (getBlock().getType() == Material.DIODE_BLOCK_ON) {
                    getBlock().setTypeIdAndData(Material.DIODE_BLOCK_OFF.getId(), (byte) (facing + 4 * (delay - 1)), true);
                }
            }
        }.runTaskLater(Tekkit.getInstance(), 3);
    }

    @Override
    public void onEnable() {
        if (!this.getBlock().getType().name().contains("DIODE")) {
            this.destroy(false);
            return;
        }

        this.updateTask(5 * this.delay);

        Diode diode = (Diode) getBlock().getState().getData();
        BlockFace facing = diode.getFacing();
        this.facing = (facing == BlockFace.SOUTH ? 2 : facing == BlockFace.EAST ? 1 : facing == BlockFace.NORTH ? 0 : 3);
        this.delay = diode.getDelay();
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        return new ArrayList<ItemStack>();
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
    public String getName() {
        return "RedstoneTimer";
    }

    @Override
    public boolean doDrop() {
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                !event.getPlayer().isSneaking() &&
                event.getClickedBlock().getLocation().equals(this.getLocation())) {
            event.setCancelled(true);
            this.delay = (this.delay == 4 ? 1 : this.delay + 1);
            this.getBlock().setData((byte) (4 * (this.delay - 1) + this.facing));
            this.updateTask(5 * this.delay);
        }
    }

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (event.getBlock().getLocation().equals(this.getLocation())) {
            event.setNewCurrent(1);
        }
    }
}