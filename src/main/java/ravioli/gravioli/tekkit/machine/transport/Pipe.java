package ravioli.gravioli.tekkit.machine.transport;

import org.bukkit.block.BlockFace;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.utilities.Persistent;

public abstract class Pipe extends MachineBase {
    @Persistent
    protected MovingItemSet items = new MovingItemSet();

    protected PipeTransport transport;

    public Pipe(PipeTransport transport) {
        this.transport = transport;
        for (int i = 0; i < BlockFace.values().length; i++) {
            BlockFace face = BlockFace.values()[i];
            if (!face.name().contains("_") && face != BlockFace.SELF) {
                this.acceptableInputs[i] = true;
                this.acceptableOutputs[i] = true;
            }
        }
    }

    public void addItem(MovingItem item, BlockFace input) {
        this.transport.addItem(item, input);
    }

    public abstract double getSpeed();
}