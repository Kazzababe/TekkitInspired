package ravioli.gravioli.tekkit.machine.transport;

import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public abstract class PipeTransport {
    public Pipe container;

    public abstract void addItem(MovingItem paramMovingItem, BlockFace paramBlockFace);

    public abstract ArrayList<ItemStack> getDrops();

    public abstract void update();

    public abstract void destroy();

    public void setContainer(Pipe pipe) {
        this.container = pipe;
    }
}