package ravioli.gravioli.tekkit.machine.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
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
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.MachineWithInventory;
import ravioli.gravioli.tekkit.machine.utilities.Fuel;
import ravioli.gravioli.tekkit.machine.utilities.Persistent;
import ravioli.gravioli.tekkit.util.CommonUtils;

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
            return this.speed;
        }
    }

    @Persistent
    private Stage stage = Stage.BUILDING;

    @Persistent
    private int armY;

    @Persistent
    private int armX = -1;

    @Persistent
    private int armZ;

    @Persistent
    private long fuelDuration;

    @Persistent
    private Location corner1;

    @Persistent
    private Location corner2;

    private ArrayList<Location> edge = new ArrayList<Location>();
    private ArrayList<Location> arm = new ArrayList<Location>();

    public MachineQuarry() {
        super("Quarry", 9);
        for (int i = 0; i < BlockFace.values().length; ++i) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                this.acceptableInputs[i] = true;
                this.acceptableOutputs[i] = true;
            }
        }
    }

    private void checkForFuel() {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(this.getInventory().getContents()));
        Optional<ItemStack> optional = items.stream().filter(item -> item != null && Fuel.FUELS.containsKey(item.getType())).findFirst();
        if (optional.isPresent()) {
            ItemStack item = optional.get();
            this.fuelDuration = (long) (Fuel.FUELS.get(item.getType()).getDuration() / 2.5);
            this.getInventory().removeItem(new ItemStack(item.getType(), 1, item.getDurability()));
        }
    }

    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack item, BlockFace input) {
        return this.getInventory().addItem(item);
    }

    @Override
    public void run() {
        if (this.stage != Stage.BROKEN) {
            if (this.fuelDuration <= 0) {
                this.checkForFuel();
                return;
            }
            this.fuelDuration -= 1000;
        }
        if (this.stage == Stage.BUILDING) {
            boolean finished = true;
            for (Location loc : this.edge) {
                Block block = loc.getBlock();

                if (block.hasMetadata("quarry-frame")) {
                    continue;
                }
                if (block.getType() == Material.BEDROCK) {
                    this.breakMachine();
                    return;
                }
                if (block.getType() != Material.AIR) {
                    ArrayList<ItemStack> drops = new ArrayList<ItemStack> ();
                    MachineBase machine = Tekkit.getMachineManager().getMachineByLocation(loc);
                    if (machine != null) {
                        drops.addAll(machine.getDrops());
                        if (machine.doDrop()) {
                            drops.add(machine.getRecipe().getResult());
                        }
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
                        MachineBase check = this.checkBlockMachinePiece(block);
                        if (check != null) {
                            check.onMachinePieceBreak(block);
                        }
                    }
                    drops.forEach(drop -> this.routeItem(BlockFace.UP, drop));
                }
                block.setType(Material.COBBLE_WALL);
                block.setMetadata("quarry-frame", new FixedMetadataValue(Tekkit.getInstance(), this));
                block.setMetadata("machine", new FixedMetadataValue(Tekkit.getInstance(), this));
                finished = false;
                break;
            }
            if (finished) {
                this.stage = Stage.CLEARING;
                this.updateTask(this.stage.getSpeed());
                this.saveAsync();
            }
        } else if (this.stage == Stage.CLEARING) {
            boolean finished = true;
            ArrayList<Location> toClear = CommonUtils.getBlocksInCuboid(this.corner1, this.corner2);
            for (Location loc : toClear) {
                if (this.edge.contains(loc) || loc.getBlock().getType() == Material.AIR) {
                    continue;
                }
                Block block = loc.getBlock();
                if (block.getType() == Material.BEDROCK) {
                    this.breakMachine();
                    return;
                }
                if (block.getType() != Material.AIR) {
                    ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                    MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(loc);
                    if (machine != null) {
                        drops.addAll(machine.getDrops());
                        if (machine.doDrop()) {
                            drops.add(machine.getRecipe().getResult());
                        }
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
                        MachineBase check = this.checkBlockMachinePiece(block);
                        if (check != null) {
                            check.onMachinePieceBreak(block);
                        }
                        block.setTypeIdAndData(0, (byte) 0, true);
                    }
                    drops.forEach(drop -> this.routeItem(BlockFace.UP, drop));
                }
                finished = false;
                break;
            }
            if (finished) {
                this.stage = Stage.MINING;
                this.updateTask((long) this.stage.getSpeed());
                this.saveAsync();
            }
        } else if (this.stage == Stage.MINING) {
            for (Location loc: this.arm) {
                loc.getBlock().setTypeIdAndData(0, (byte) 0, true);
                if (loc.getBlock().hasMetadata("machine")) {
                    loc.getBlock().removeMetadata("machine", Tekkit.getInstance());
                }
                if (loc.getBlock().hasMetadata("quarry-arm")) {
                    loc.getBlock().removeMetadata("quarry-arm", Tekkit.getInstance());
                }
            }
            this.arm.clear();

            int width = Math.abs(this.corner1.getBlockX() - this.corner2.getBlockX()) - 1;
            int length = Math.abs(this.corner1.getBlockZ() - this.corner2.getBlockZ()) - 1;
            int height = Math.abs(this.corner1.getBlockY() - this.corner2.getBlockY());

            int diffX = this.corner1.getBlockX() - this.corner2.getBlockX() < 0 ? -1 : 1;
            int diffZ = this.corner1.getBlockZ() - this.corner2.getBlockZ() < 0 ? -1 : 1;

            this.armX++;
            if (this.armX >= width) {
                this.armX = 0;
                this.armZ++;
                if (this.armZ >= length) {
                    this.armZ = 0;
                    this.armY++;
                }
            }
            this.calculateArm();

            for (Location loc: this.arm) {
                loc.getBlock().setType(Material.COBBLE_WALL);
                loc.getBlock().setMetadata("quarry-arm", new FixedMetadataValue(Tekkit.getInstance(), this));
                loc.getBlock().setMetadata("machine", new FixedMetadataValue(Tekkit.getInstance(), this));
            }
            this.corner2.clone().add(diffX + this.armX * diffX, 0, diffZ + this.armZ * diffZ).getBlock().setType(Material.IRON_BLOCK);
            this.corner2.clone().add(diffX + this.armX * diffX, -height - this.armY, diffZ + this.armZ * diffZ).getBlock().setType(Material.HOPPER);
            this.corner2.clone().add(diffX + this.armX * diffX, -height - this.armY + 1, diffZ + this.armZ * diffZ).getBlock().setType(Material.IRON_BLOCK);

            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            Location target = this.corner2.clone().add(diffX + this.armX * diffX, -height - this.armY - 1, diffZ + this.armZ * diffZ);
            Block targetBlock = target.getBlock();
            if (targetBlock.getType() == Material.BEDROCK) {
                this.breakMachine();
                return;
            }
            MachineBase machine = Tekkit.getMachineManager().getMachineByLocation(target);
            if (machine != null) {
                drops.addAll(machine.getDrops());
                if (machine.doDrop()) {
                    drops.add(machine.getRecipe().getResult());
                }
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
                MachineBase check = this.checkBlockMachinePiece(targetBlock);
                if (check != null) {
                    check.onMachinePieceBreak(targetBlock);
                }
                targetBlock.setTypeIdAndData(0, (byte) 0, true);
            }
            drops.forEach(drop -> this.routeItem(BlockFace.UP, drop));
            this.saveAsync();
        } else if (this.stage == Stage.BROKEN) {
            this.stopTask();
        }
    }

    private void calculateArm() {
        int width = Math.abs(this.corner1.getBlockX() - this.corner2.getBlockX()) - 1;
        int length = Math.abs(this.corner1.getBlockZ() - this.corner2.getBlockZ()) - 1;
        int height = Math.abs(this.corner1.getBlockY() - this.corner2.getBlockY());

        int diffX = this.corner1.getBlockX() - this.corner2.getBlockX() < 0 ? -1 : 1;
        int diffZ = this.corner1.getBlockZ() - this.corner2.getBlockZ() < 0 ? -1 : 1;

        for (int x = 1; x <= width; x++) {
            this.arm.add(this.corner2.clone().add(x * diffX, 0, diffZ + this.armZ * diffZ));
        }
        for (int z = 1; z <= length; z++) {
            this.arm.add(this.corner2.clone().add(diffX + this.armX * diffX, 0, z * diffZ));
        }
        for (int y = 1; y <= height + this.armY; y++) {
            this.arm.add(this.corner2.clone().add(diffX + this.armX * diffX, -y, diffZ + this.armZ * diffZ));
        }
    }

    private void breakMachine() {
        this.stage = Stage.BROKEN;
        this.stopTask();
        this.saveAsync();
    }

    @Override
    public void onMachinePieceBreak(Block block) {
        if (block.hasMetadata("quarry-frame")) {
            block.removeMetadata("quarry-frame", Tekkit.getInstance());
            this.stage = Stage.BROKEN;
            this.saveAsync();
        }
    }

    @Override
    public void onCreate() {
        Player player = Bukkit.getPlayer(this.getOwner());
        if (player == null) {
            this.destroy(true);
            return;
        }
        this.calculateCorners(CommonUtils.getCardinalDirection(player));
    }

    @Override
    public void onDestroy() {
        for (Location loc : this.edge) {
            Block block = loc.getBlock();
            block.setType(Material.AIR);
            if (block.hasMetadata("quarry-frame")) {
                block.removeMetadata("quarry-frame", Tekkit.getInstance());
            }
            if (block.hasMetadata("machine")) {
                block.removeMetadata("machine", Tekkit.getInstance());
            }
        }
        for (Location loc : this.arm) {
            Block block = loc.getBlock();
            block.setType(Material.AIR);
            if (block.hasMetadata("machine")) {
                block.removeMetadata("machine", Tekkit.getInstance());
            }
            if (block.hasMetadata("quarry-arm")) {
                block.removeMetadata("quarry-arm", Tekkit.getInstance());
            }
            if (block.hasMetadata("machine")) {
                block.removeMetadata("machine", Tekkit.getInstance());
            }
        }
    }

    @Override
    public void onEnable() {
        this.updateTask(this.stage.getSpeed());
        this.updateEdge();
        if (this.stage == Stage.MINING || this.stage == Stage.BROKEN) {
            this.calculateArm();
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
    public String getName() {
        return "Quarry";
    }

    @Override
    public boolean doDrop() {
        return true;
    }

    private void calculateCorners(BlockFace direction) {
        Block facing = this.getLocation().getBlock().getRelative(direction);
        if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
            int difference = this.getLocation().getBlockX() - facing.getX();
            this.corner1 = facing.getLocation().clone().add(-8 * difference, 0, -4);
            this.corner2 = facing.getLocation().clone().add(0, 5, 4);
        } else if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
            int difference = this.getLocation().getBlockZ() - facing.getZ();
            this.corner1 = facing.getLocation().clone().add(-4, 0, -8 * difference);
            this.corner2 = facing.getLocation().clone().add(4, 5, 0);
        }
        CommonUtils.minMaxCorners(this.corner1, this.corner2);
    }

    private void updateEdge() {
        this.edge = CommonUtils.getPointsInRegion(this.corner1, this.corner2);
        if (this.stage != Stage.BROKEN && this.stage != Stage.BUILDING) {
            for (Location loc : this.edge) {
                loc.getBlock().setMetadata("quarry-frame", new FixedMetadataValue(Tekkit.getInstance(), this));
                loc.getBlock().setMetadata("machine", new FixedMetadataValue(Tekkit.getInstance(), this));
            }
        }
    }
}