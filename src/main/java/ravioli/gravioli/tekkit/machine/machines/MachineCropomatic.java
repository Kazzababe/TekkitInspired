package ravioli.gravioli.tekkit.machine.machines;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineWithInventory;
import ravioli.gravioli.tekkit.util.CommonUtils;
import ravioli.gravioli.tekkit.util.InventoryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class MachineCropomatic extends MachineWithInventory {
    private Location corner1;
    private Location corner2;

    private ArrayList<Location> queue = new ArrayList<Location>();

    public MachineCropomatic() {
        super("Cropomatic", 9 * 3);

        for (int i = 0; i < BlockFace.values().length; ++i) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                this.acceptableOutputs[i] = true;
            }
        }
    }

    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack item, BlockFace input) {
        return new HashMap<Integer, ItemStack>();
    }

    @Override
    public void run() {
        Iterator<Location> iterator = this.queue.iterator();
        while(iterator.hasNext()) {
            Location location = iterator.next();
            Block block = location.getBlock();
            Material type = block.getType();

            if (type == Material.CROPS) {
                if (block.getData() == (byte) 7) {
                    if (InventoryUtils.canFitIntoInventory(this.getInventory(), new ItemStack(Material.SEEDS))) {
                        this.routeCrop(block);
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onEnable() {
        this.updateTask(20);

        this.corner1 = this.getLocation().clone().add(10, 10, 10);
        this.corner2 = this.getLocation().clone().subtract(10, 10, 10);
        CommonUtils.minMaxCorners(this.corner1, this.corner2);

        for (int x = this.corner1.getBlockX(); x <= this.corner2.getBlockX(); x++) {
            for (int y = this.corner1.getBlockY(); y <= this.corner2.getBlockY(); y++) {
                for (int z = this.corner1.getBlockZ(); z <= this.corner2.getBlockZ(); z++) {
                    Location location = new Location(this.getBlock().getWorld(), x, y, z);
                    Block block = location.getBlock();
                    Material type = block.getType();

                    if (type == Material.CROPS) {
                        if (block.getData() == (byte) 7) {
                            this.queue.add(location);
                        }
                    } else if (type == Material.MELON_BLOCK || type == Material.PUMPKIN) {
                        this.queue.add(location);
                    }
                }
            }
        }
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Crop-o-matic");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("IPI", "IHI", "IEI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('P', Material.IRON_HOE);
        recipe.setIngredient('H', Material.HOPPER);
        recipe.setIngredient('E', Material.ENDER_PEARL);

        return recipe;
    }

    @Override
    public String getName() {
        return "Cropomatic";
    }

    @Override
    public boolean doDrop() {
        return true;
    }

    @EventHandler
    public void onStructureGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        BlockState newState = event.getNewState();

        if (block.getLocation().toVector().isInAABB(this.corner1.toVector(), this.corner2.toVector())) {
            if (block.getType() == Material.CROPS) {
                if (event.getNewState().getRawData() == (byte) 7) {
                    if (InventoryUtils.canFitIntoInventory(this.getInventory(), new ItemStack(Material.SEEDS))) {
                        event.setCancelled(true);
                        block.setData((byte) 7);
                        this.routeCrop(block);
                    } else {
                        if (!this.queue.contains(block.getLocation())) {
                            this.queue.add(block.getLocation());
                        }
                    }
                }
            } else if (newState.getType() == Material.MELON_BLOCK || newState.getType() == Material.PUMPKIN) {
                event.setCancelled(true);
                block.setType(newState.getType());
                this.routeCrop(newState.getBlock());
            }
        }
    }

    private void routeCrop(Block block) {
        Bukkit.getScheduler().runTaskLater(Tekkit.getInstance(), new UpdateCropTask(this, block), 10);
    }

    private class UpdateCropTask implements Runnable {
        private MachineCropomatic machine;
        private Block block;
        private Material originalType;
        private byte data;

        private UpdateCropTask(MachineCropomatic machine, Block block) {
            this.machine = machine;
            this.block = block;
            this.originalType = block.getType();
            this.data = block.getData();
        }

        @Override
        public void run() {
            if (this.block.getType() != this.originalType || this.block.getData() != this.data) {
                return;
            }

            Material type = this.block.getType();
            Collection<ItemStack> drops = this.block.getDrops();

            this.block.setTypeIdAndData(0, (byte) 0, true);
            switch (type) {
                case CROPS:
                    drops.add(new ItemStack(Material.SEEDS));
                    this.block.setType(Material.CROPS);
                    break;
            }
            this.block.getWorld().playEffect(this.block.getLocation(), Effect.SMOKE, (byte) 4);

            drops.forEach(drop -> {
               if (drop.getType() == Material.SEEDS) {
                   this.machine.getInventory().addItem(drop);
               } else {
                   this.machine.routeItem(BlockFace.UP, drop);
               }
            });
        }
    }
}
