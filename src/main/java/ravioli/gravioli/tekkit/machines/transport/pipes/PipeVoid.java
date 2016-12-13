package ravioli.gravioli.tekkit.machines.transport.pipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.transport.MovingItem;
import ravioli.gravioli.tekkit.machines.transport.PipeTransportGeneric;

public class PipeVoid extends Pipe {
    public PipeVoid(Tekkit plugin) {
        super(plugin, new PipeTransportGeneric());
    }

    @Override
    public void runMachine() {
        transport.update();
    }

    @Override
    public void onEnable() {
        updateTask(1);
        getWorld().getNearbyEntities(getLocation(), 2.0, 2.0, 2.0).stream().filter(entity -> entity instanceof ArmorStand && !entity.hasMetadata("display") && ((ArmorStand) entity).isMarker()).forEach(entity -> entity.remove());
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS, 1, (byte) 10);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Void Pipe");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("WGW");
        recipe.setIngredient('W', Material.ENDER_PEARL);
        recipe.setIngredient('G', Material.GLASS);

        return recipe;
    }

    @Override
    public String getTableName() {
        return "VoidPipe";
    }

    @Override
    public String getName() {
        return "voidpipe";
    }

    @Override
    public double getSpeed() {
        return 0.15;
    }

    @Override
    public void addItem(MovingItem item, BlockFace input) {
        item.destroy();
    }
}
