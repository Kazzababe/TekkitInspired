package ravioli.gravioli.tekkit.machines.transport.pipes;

import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.transport.MovingItem;
import ravioli.gravioli.tekkit.machines.transport.MovingItemSet;
import ravioli.gravioli.tekkit.machines.transport.PipeTransport;
import ravioli.gravioli.tekkit.machines.transport.TransportReceiver;

import java.util.ArrayList;
import java.util.List;

public abstract class Pipe extends Machine implements TransportReceiver {
    @DatabaseObject
    private MovingItemSet itemSet = new MovingItemSet();
    protected PipeTransport transport;

    public Pipe(Tekkit plugin, PipeTransport transport) {
        super(plugin);

        this.transport = transport;
        transport.container = this;

        for (int i = 0; i < BlockFace.values().length; i++) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                this.acceptableInputs[i] = true;
                this.acceptableOutputs[i] = true;
            }
        }
    }

    public void addItem(MovingItem item, BlockFace input) {
        transport.addItem(item, input);
    }

    public MovingItemSet getItemSet() {
        return itemSet;
    }

    @Override
    public boolean canReceiveItem(MovingItem item, BlockFace input) {
        return acceptableInput(input);
    }

    @Override
    public void addMovingItem(MovingItem item, BlockFace input) {
        addItem(item, input);
    }

    @Override
    public void onDestroy() {
        transport.destroy();
    }

    @Override
    public List<ItemStack> getDrops() {
        return new ArrayList();
    }

    /**
     * The speed at which items move through the pipe
     *
     * @return item speed
     */
    public abstract double getSpeed();
}
