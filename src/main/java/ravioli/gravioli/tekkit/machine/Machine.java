package ravioli.gravioli.tekkit.machine;

import java.util.ArrayList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public interface Machine {
    public ArrayList<ItemStack> getDrops();

    public Recipe getRecipe();

    public String getName();

    public boolean doDrop();
}


