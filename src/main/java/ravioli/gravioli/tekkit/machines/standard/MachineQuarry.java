package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachineWithInventory;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.utils.Fuel;
import ravioli.gravioli.tekkit.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class MachineQuarry extends MachineWithInventory {
    enum Stage {
        BUILDING(10),
        CLEARING(10),
        MINING(15),
        BROKEN(0);

        private int speed;

        Stage(int speed) {
            this.speed = speed;
        }

        public int getSpeed() {
            return speed;
        }
    }

    @DatabaseObject
    private Stage stage = Stage.BUILDING;

    @DatabaseObject
    private int armY;

    @DatabaseObject
    private int armX = -1;

    @DatabaseObject
    private int armZ;

    @DatabaseObject
    private long fuelDuration;

    @DatabaseObject
    private Location corner1;

    @DatabaseObject
    private Location corner2;

    private ArrayList<Location> edge = new ArrayList<Location>();
    private ArrayList<Location> arm = new ArrayList<Location>();

    public MachineQuarry(Tekkit plugin) {
        super(plugin, "Quarry", 9);
        for (int i = 0; i < BlockFace.values().length; ++i) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                acceptableInputs[i] = true;
                acceptableOutputs[i] = true;
            }
        }
    }

    private void checkForFuel() {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(getInventory().getContents()));
        Optional<ItemStack> optional = items.stream().filter(item -> item != null && Fuel.FUELS.containsKey(item.getType())).findFirst();
        if (optional.isPresent()) {
            ItemStack item = optional.get();
            fuelDuration = (long) (Fuel.FUELS.get(item.getType()).getDuration() / 2.5);
            getInventory().removeItem(new ItemStack(item.getType(), 1, item.getDurability()));
        }
    }

    @Override
    public void runMachine() {
        if (stage != Stage.BROKEN) {
            if (fuelDuration <= 0) {
                checkForFuel();
                return;
            }
            fuelDuration -= 1000;
        }
        if (stage == Stage.BUILDING) {
            boolean finished = true;
            for (Location loc : edge) {
                Block block = loc.getBlock();

                if (block.hasMetadata("quarry-frame")) {
                    continue;
                }
                if (block.getType() == Material.BEDROCK) {
                    breakMachine();
                    return;
                }
                if (block.getType() != Material.AIR) {
                    ArrayList<ItemStack> drops = new ArrayList<ItemStack> ();
                    Machine machine = MachinesManager.getMachineByLocation(loc);
                    if (machine != null) {
                        drops.addAll(machine.getDrops());
                        drops.add(machine.getRecipe().getResult());
                        machine.destroy(false);
                    } else if (!block.hasMetadata("machine")) {
                        drops.addAll(block.getDrops(new ItemStack(Material.DIAMOND_PICKAXE)));
                        if (block.getState() instanceof InventoryHolder) {
                            InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
                            ArrayList<ItemStack> items = new ArrayList<ItemStack> (Arrays.asList(inventoryHolder.getInventory().getContents()));
                            items.removeAll(Arrays.asList("", null));
                            drops.addAll(items);
                        }
                    } else {
                        Machine check = checkBlockMachinePiece(block);
                        if (check != null) {
                            check.onMachinePieceBreak(block);
                        }
                    }
                    drops.forEach(drop -> routeItem(BlockFace.UP, drop));
                }
                block.setType(Material.COBBLE_WALL);
                block.setMetadata("quarry-frame", new FixedMetadataValue(getPlugin(), this));
                block.setMetadata("machine", new FixedMetadataValue(getPlugin(), this));
                finished = false;
                break;
            }
            if (finished) {
                stage = Stage.CLEARING;
                updateTask(stage.getSpeed());
                saveAsync();
            }
        } else if (stage == Stage.CLEARING) {
            boolean finished = true;
            ArrayList<Location> toClear = CommonUtils.getBlocksInCuboid(corner1, corner2);
            for (Location loc : toClear) {
                if (edge.contains(loc) || loc.getBlock().getType() == Material.AIR) {
                    continue;
                }
                Block block = loc.getBlock();
                if (block.getType() == Material.BEDROCK) {
                    breakMachine();
                    return;
                }
                if (block.getType() != Material.AIR) {
                    ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                    Machine machine = MachinesManager.getMachineByLocation(loc);
                    if (machine != null) {
                        drops.addAll(machine.getDrops());
                        drops.add(machine.getRecipe().getResult());
                        machine.destroy(false);
                    } else if (!block.hasMetadata("machine")) {
                        drops.addAll(block.getDrops(new ItemStack(Material.DIAMOND_PICKAXE)));
                        if (block.getState() instanceof InventoryHolder) {
                            InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
                            ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(inventoryHolder.getInventory().getContents()));
                            items.removeAll(Arrays.asList("", null));
                            drops.addAll(items);
                        }
                        block.setTypeIdAndData(0, (byte) 0, true);
                    } else {
                        Machine check = checkBlockMachinePiece(block);
                        if (check != null) {
                            check.onMachinePieceBreak(block);
                        }
                        block.setTypeIdAndData(0, (byte) 0, true);
                    }
                    drops.forEach(drop -> routeItem(BlockFace.UP, drop));
                }
                finished = false;
                break;
            }
            if (finished) {
                stage = Stage.MINING;
                updateTask((long) stage.getSpeed());
                saveAsync();
            }
        } else if (stage == Stage.MINING) {
            for (Location loc: arm) {
                loc.getBlock().setTypeIdAndData(0, (byte) 0, true);
                if (loc.getBlock().hasMetadata("machine")) {
                    loc.getBlock().removeMetadata("machine", getPlugin());
                }
                if (loc.getBlock().hasMetadata("quarry-arm")) {
                    loc.getBlock().removeMetadata("quarry-arm", getPlugin());
                }
            }
            arm.clear();

            int width = Math.abs(corner1.getBlockX() - corner2.getBlockX()) - 1;
            int length = Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) - 1;
            int height = Math.abs(corner1.getBlockY() - corner2.getBlockY());

            int diffX = corner1.getBlockX() - corner2.getBlockX() < 0 ? -1 : 1;
            int diffZ = corner1.getBlockZ() - corner2.getBlockZ() < 0 ? -1 : 1;

            armX++;
            if (armX >= width) {
                armX = 0;
                armZ++;
                if (armZ >= length) {
                    armZ = 0;
                    armY++;
                }
            }
            calculateArm();

            for (Location loc: arm) {
                loc.getBlock().setType(Material.COBBLE_WALL);
                loc.getBlock().setMetadata("quarry-arm", new FixedMetadataValue(getPlugin(), this));
                loc.getBlock().setMetadata("machine", new FixedMetadataValue(getPlugin(), this));
            }
            corner2.clone().add(diffX + armX * diffX, 0, diffZ + armZ * diffZ).getBlock().setType(Material.IRON_BLOCK);
            corner2.clone().add(diffX + armX * diffX, -height - armY, diffZ + armZ * diffZ).getBlock().setType(Material.HOPPER);
            corner2.clone().add(diffX + armX * diffX, -height - armY + 1, diffZ + armZ * diffZ).getBlock().setType(Material.IRON_BLOCK);

            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            Location target = corner2.clone().add(diffX + armX * diffX, -height - armY - 1, diffZ + armZ * diffZ);
            Block targetBlock = target.getBlock();
            if (targetBlock.getType() == Material.BEDROCK) {
                breakMachine();
                return;
            }
            Machine machine = MachinesManager.getMachineByLocation(target);
            if (machine != null) {
                drops.addAll(machine.getDrops());
                drops.add(machine.getRecipe().getResult());
                machine.destroy(false);
            } else if (!targetBlock.hasMetadata("machine")) {
                drops.addAll(targetBlock.getDrops(new ItemStack(Material.DIAMOND_PICKAXE)));
                if (targetBlock.getState() instanceof InventoryHolder) {
                    InventoryHolder inventoryHolder = (InventoryHolder) targetBlock.getState();
                    ArrayList<ItemStack> items = new ArrayList<ItemStack> (Arrays.asList(inventoryHolder.getInventory().getContents()));
                    items.removeAll(Arrays.asList("", null));
                    drops.addAll(items);
                }
                targetBlock.setTypeIdAndData(0, (byte) 0, true);
            } else {
                Machine check = checkBlockMachinePiece(targetBlock);
                if (check != null) {
                    check.onMachinePieceBreak(targetBlock);
                }
                targetBlock.setTypeIdAndData(0, (byte) 0, true);
            }
            drops.forEach(drop -> routeItem(BlockFace.UP, drop));
            saveAsync();
        } else if (stage == Stage.BROKEN) {
            stopTask();
        }
    }

    private void calculateArm() {
        int width = Math.abs(corner1.getBlockX() - corner2.getBlockX()) - 1;
        int length = Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) - 1;
        int height = Math.abs(corner1.getBlockY() - corner2.getBlockY());

        int diffX = corner1.getBlockX() - corner2.getBlockX() < 0 ? -1 : 1;
        int diffZ = corner1.getBlockZ() - corner2.getBlockZ() < 0 ? -1 : 1;

        for (int x = 1; x <= width; x++) {
            arm.add(corner2.clone().add(x * diffX, 0, diffZ + armZ * diffZ));
        }
        for (int z = 1; z <= length; z++) {
            arm.add(corner2.clone().add(diffX + armX * diffX, 0, z * diffZ));
        }
        for (int y = 1; y <= height + armY; y++) {
            arm.add(corner2.clone().add(diffX + armX * diffX, -y, diffZ + armZ * diffZ));
        }
        for (Location loc : arm) {
            loc.getBlock().setMetadata("quarry-arm", new FixedMetadataValue(getPlugin(), this));
            loc.getBlock().setMetadata("machine", new FixedMetadataValue(getPlugin(), this));
        }
    }

    private void breakMachine() {
        stage = Stage.BROKEN;
        stopTask();
        saveAsync();
    }

    @Override
    public void onMachinePieceBreak(Block block) {
        if (block.hasMetadata("quarry-frame")) {
            block.removeMetadata("quarry-frame", getPlugin());
            stage = Stage.BROKEN;
            saveAsync();
        }
    }

    @Override
    public void onCreate() {
        Player player = Bukkit.getPlayer(getOwner());
        if (player == null) {
            destroy(true);
            return;
        }
        calculateCorners(CommonUtils.getCardinalDirection(player));
    }

    @Override
    public void onDestroy() {
        for (Location loc : edge) {
            Block block = loc.getBlock();
            if (block.hasMetadata("quarry-frame")) {
                block.removeMetadata("quarry-frame", getPlugin());
                block.setType(Material.AIR);
            }
            if (block.hasMetadata("machine")) {
                block.removeMetadata("machine", getPlugin());
            }
        }
        for (Location loc : arm) {
            Block block = loc.getBlock();
            if (block.hasMetadata("quarry-arm")) {
                block.removeMetadata("quarry-arm", getPlugin());
                block.setType(Material.AIR);
            }
            if (block.hasMetadata("machine")) {
                block.removeMetadata("machine", getPlugin());
            }
        }
    }

    @Override
    public void onEnable() {
        updateTask(stage.getSpeed());
        updateEdge();
        if (stage == Stage.MINING || stage == Stage.BROKEN) {
            calculateArm();
        }
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Quarry");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("IDI", "IPI", "IDI");
        recipe.setIngredient('I', Material.IRON_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('P', Material.IRON_PICKAXE);

        return recipe;
    }

    @Override
    public String getTableName() {
        return "Quarry";
    }

    @Override
    public String getName() {
        return "quarry";
    }

    private void calculateCorners(BlockFace direction) {
        Block facing = getLocation().getBlock().getRelative(direction);
        if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
            int difference = getLocation().getBlockX() - facing.getX();
            corner1 = facing.getLocation().clone().add(-8 * difference, 0, -4);
            corner2 = facing.getLocation().clone().add(0, 5, 4);
        } else if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
            int difference = getLocation().getBlockZ() - facing.getZ();
            corner1 = facing.getLocation().clone().add(-4, 0, -8 * difference);
            corner2 = facing.getLocation().clone().add(4, 5, 0);
        }
        CommonUtils.minMaxCorners(corner1, corner2);
    }

    private void updateEdge() {
        edge = CommonUtils.getPointsInRegion(corner1, corner2);
        if (stage != Stage.BUILDING) {
            for (Location loc : edge) {
                loc.getBlock().setMetadata("quarry-frame", new FixedMetadataValue(getPlugin(), this));
                loc.getBlock().setMetadata("machine", new FixedMetadataValue(getPlugin(), this));
            }
        }
    }
}
