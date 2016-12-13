package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.Machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MachineWorldAnchor extends Machine {
    public MachineWorldAnchor(Tekkit plugin) {
        super(plugin);
    }

    @Override
    public void runMachine() {

    }

    @Override
    public List<ItemStack> getDrops() {
        return new ArrayList();
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.OBSIDIAN);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "World Anchor");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("GOG", "DOD", "GOG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('D', Material.DIAMOND);

        return recipe;
    }

    @Override
    public String getTableName() {
        return "WorldAnchor";
    }

    @Override
    public String getName() {
        return "worldanchor";
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk machineChunk = getBlock().getChunk();
        Chunk chunk = event.getChunk();

        if (Math.hypot(chunk.getX() - machineChunk.getX(), chunk.getZ() - machineChunk.getZ()) <= 3) {
            event.setCancelled(true);
        }
    }
}
