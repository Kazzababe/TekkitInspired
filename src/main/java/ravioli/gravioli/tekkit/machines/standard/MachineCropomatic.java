package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.MachineWithInventory;
import ravioli.gravioli.tekkit.utils.CommonUtils;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MachineCropomatic extends MachineWithInventory {
    private Location corner1;
    private Location corner2;

    private ArrayList<Location> queue = new ArrayList<Location>();

    public MachineCropomatic(Tekkit plugin) {
        super(plugin, "Crop-o-matic", 9 * 3);

        for (int i = 0; i < BlockFace.values().length; ++i) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                this.acceptableOutputs[i] = true;
            }
        }
    }

    @Override
    public void runMachine() {
        Iterator<Location> iterator = this.queue.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            Block block = location.getBlock();
            Material type = block.getType();

            if (type == Material.CROPS) {
                if (block.getData() == (byte) 7) {
                    if (InventoryUtils.canFitIntoInventory(getInventory(), new ItemStack(Material.SEEDS))) {
                        routeCrop(block);
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        updateTask(20);

        corner1 = this.getLocation().clone().add(10, 10, 10);
        corner2 = this.getLocation().clone().subtract(10, 10, 10);
        CommonUtils.minMaxCorners(corner1, corner2);

        for (int x = corner1.getBlockX(); x <= corner2.getBlockX(); x++) {
            for (int y = corner1.getBlockY(); y <= corner2.getBlockY(); y++) {
                for (int z = corner1.getBlockZ(); z <= corner2.getBlockZ(); z++) {
                    Location location = new Location(getWorld(), x, y, z);
                    Block block = location.getBlock();
                    Material type = block.getType();

                    if (type == Material.CROPS) {
                        if (block.getData() == (byte) 7) {
                            queue.add(location);
                        }
                    } else if (type == Material.MELON_BLOCK || type == Material.PUMPKIN) {
                        queue.add(location);
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
    public String getTableName() {
        return "Cropomatic";
    }

    @Override
    public String getName() {
        return "cropomatic";
    }

    @EventHandler
    public void onStructureGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        BlockState newState = event.getNewState();

        if (block.getLocation().toVector().isInAABB(corner1.toVector(), corner2.toVector())) {
            if (block.getType() == Material.CROPS) {
                if (event.getNewState().getRawData() == (byte) 7) {
                    if (InventoryUtils.canFitIntoInventory(this.getInventory(), new ItemStack(Material.SEEDS))) {
                        event.setCancelled(true);
                        block.setData((byte) 7);
                        routeCrop(block);
                    } else {
                        if (!queue.contains(block.getLocation())) {
                            queue.add(block.getLocation());
                        }
                    }
                }
            } else if (newState.getType() == Material.MELON_BLOCK || newState.getType() == Material.PUMPKIN) {
                event.setCancelled(true);
                block.setType(newState.getType());
                routeCrop(newState.getBlock());
            }
        }
    }

    private void routeCrop(Block block) {
        Bukkit.getScheduler().runTaskLater(getPlugin(), new UpdateCropTask(this, block), 10);
    }

    private class UpdateCropTask implements Runnable {
        private MachineCropomatic machine;
        private Block block;
        private Material originalType;
        private byte data;

        private UpdateCropTask(MachineCropomatic machine, Block block) {
            this.machine = machine;
            this.block = block;
            originalType = block.getType();
            data = block.getData();
        }

        @Override
        public void run() {
            if (block.getType() != originalType || block.getData() != data) {
                return;
            }

            Material type = block.getType();
            Collection<ItemStack> drops = block.getDrops();

            block.setTypeIdAndData(0, (byte) 0, true);
            switch (type) {
                case CROPS:
                    drops.add(new ItemStack(Material.SEEDS));
                    block.setType(Material.CROPS);
                    break;
            }
            block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, (byte) 4);

            drops.forEach(drop -> {
                if (drop.getType() == Material.SEEDS) {
                    machine.getInventory().addItem(drop);
                } else {
                    machine.routeItem(BlockFace.UP, drop);
                }
            });
        }
    }
}
