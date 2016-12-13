package ravioli.gravioli.tekkit.machines.transport;

import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.machines.transport.pipes.Pipe;

import java.util.ArrayList;

public abstract class PipeTransport {
    public Pipe container;

    public abstract void addItem(MovingItem item, BlockFace input);

    public abstract ArrayList<ItemStack> getDrops();

    public abstract void update();

    public abstract void destroy();

    public World getWorld() {
        return container.getWorld();
    }
}
