package ravioli.gravioli.tekkit.machines.standard;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachineWithInventory;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.utils.Fuel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class MachineMiningWell extends MachineWithInventory {
    @DatabaseObject
    private int height;
    @DatabaseObject
    private long fuelDuration;

    public MachineMiningWell(Tekkit plugin) {
        super(plugin, "Mining Well", 9);
    }

    /**
     * Checks if the mining well's inventory has a piece of fuel in it
     *
     * @return whether the machine's fuel duration was refreshed
     */
    private boolean checkForFuel() {
        ArrayList<ItemStack> items = new ArrayList(Arrays.asList(getInventory().getContents()));
        Optional<ItemStack> optional = items.stream().filter(item -> item != null && Fuel.FUELS.containsKey(item.getType())).findFirst();
        if (optional.isPresent()) {
            ItemStack item = optional.get();
            fuelDuration = (long) (Fuel.FUELS.get(item.getType()).getDuration() / 2.5);
            getInventory().removeItem(new ItemStack(item.getType(), 1, item.getDurability()));
            return true;
        }
        return false;
    }

    @Override
    public void runMachine() {
        Location location = getLocation().clone().subtract(0, height + 1, 0);
        if (location.getBlock().getType() != Material.BEDROCK && location.getY() > 0) {
            if (fuelDuration <= 0) {
                if (!checkForFuel()) {
                    return;
                }
            }
            fuelDuration -= 1000;
            height++;

            Machine machine = MachinesManager.getMachineByLocation(location);
            if (machine != null) {
                machine.getDrops().forEach(drop -> routeItem(BlockFace.UP, drop));
                routeItem(BlockFace.UP, machine.getRecipe().getResult());
                machine.destroy(false);
            } else if (!location.getBlock().hasMetadata("machine")) {
                location.getBlock().getDrops().forEach(drop -> routeItem(BlockFace.UP, drop));
            }
            location.getBlock().setType(Material.COBBLE_WALL);
            location.getBlock().setMetadata("machine", new FixedMetadataValue(getPlugin(), this));

            getWorld().playEffect(getLocation(), Effect.SMOKE, 4);
        }
    }

    @Override
    public void onEnable() {
        this.updateTask(20);
        if (height > 0) {
            for (int i = 1; i <= height; ++i) {
                Location location = getLocation().clone().subtract(0, i, 0);
                if (location.getBlock().getType() == Material.COBBLE_WALL) {
                    location.getBlock().setMetadata("machine", new FixedMetadataValue(getPlugin(), this));
                }
            }
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
    public String getTableName() {
        return "MiningWell";
    }

    @Override
    public String getName() {
        return "miningwell";
    }
}
