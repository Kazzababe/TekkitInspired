package ravioli.gravioli.tekkit.machine.transport.pipes;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import ravioli.gravioli.tekkit.machine.transport.Pipe;
import ravioli.gravioli.tekkit.machine.transport.PipeTransportGeneric;

public class PipeSimple extends Pipe {
    public PipeSimple() {
        super(new PipeTransportGeneric());
        this.transport.setContainer(this);
    }

    public void run() {
        this.transport.update();
    }

    public void onCreate() {

    }

    public void onDestroy() {
        this.transport.destroy();
    }

    public void onEnable() {
        this.updateTask(1);
        this.getLocation().getWorld().getNearbyEntities(this.getLocation(), 1.0, 2.0, 1.0).stream().filter(entity -> entity instanceof ArmorStand && !entity.hasMetadata("display") && ((ArmorStand) entity).isMarker()).forEach(entity -> entity.remove());

        if (this.getBlock().getType() != Material.GLASS) {
            this.destroy(false);
        }
    }

    public ArrayList<ItemStack> getDrops() {
        return this.transport.getDrops();
    }

    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Wooden Pipe");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("WGW");
        recipe.setIngredient('W', Material.WOOD);
        recipe.setIngredient('G', Material.GLASS);

        return recipe;
    }

    public String getName() {
        return "BasicPipe";
    }

    public boolean doDrop() {
        return true;
    }

    public double getSpeed() {
        return 0.05;
    }
}