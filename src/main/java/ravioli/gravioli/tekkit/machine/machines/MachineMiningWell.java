package ravioli.gravioli.tekkit.machine.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.MachineWithInventory;
import ravioli.gravioli.tekkit.machine.utils.Fuel;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.storage.Persistent;

public class MachineMiningWell extends MachineWithInventory {
    @Persistent
    private int height;

    @Persistent
    private long fuelDuration;

    public MachineMiningWell() {
        super("Mining Well", 9);
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
    public void onEnable() {
        if (this.getBlock().getType() != Material.IRON_BLOCK) {
            this.destroy(false);
            return;
        }

        this.updateTask(20);
        if (this.height > 0) {
            for (int i = 1; i <= this.height; ++i) {
                Location loc = this.getLocation().clone().subtract(0, i, 0);
                if (loc.getBlock().getType() == Material.COBBLE_WALL) {
                    loc.getBlock().setMetadata("machine", new FixedMetadataValue(Tekkit.getInstance(), this));
                }
            }
        }
    }

    @Override
    public void run() {
        Location loc = this.getLocation().clone().subtract(0, this.height + 1, 0);
        if (loc.getBlock().getType() != Material.BEDROCK) {
            if (this.fuelDuration <= 0) {
                this.checkForFuel();
                return;
            }
            this.fuelDuration -= 1000;
            this.height++;

            MachineBase machine = MachineManager.getMachineByLocation(loc);
            if (machine != null) {
                machine.getDrops().forEach(drop -> this.routeItem(BlockFace.UP, drop));
                this.routeItem(BlockFace.UP, machine.getRecipe().getResult());
                machine.destroy(false);
            } else if (!loc.getBlock().hasMetadata("machine")) {
                loc.getBlock().getDrops().forEach(drop -> this.routeItem(BlockFace.UP, drop));
            }
            loc.getBlock().setType(Material.COBBLE_WALL);
            loc.getBlock().setMetadata("machine", new FixedMetadataValue(Tekkit.getInstance(), this));

            this.saveAsync();
            this.getLocation().getWorld().playEffect(this.getLocation(), Effect.SMOKE, 4);
        }
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Mining Well");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("IRI", "ICI", "IPI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('C', Material.IRON_BLOCK);
        recipe.setIngredient('P', Material.IRON_PICKAXE);

        return recipe;
    }

    @Override
    public String getName() {
        return "MiningWell";
    }

    @Override
    public boolean doDrop() {
        return true;
    }
}