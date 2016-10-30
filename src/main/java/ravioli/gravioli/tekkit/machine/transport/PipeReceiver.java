package ravioli.gravioli.tekkit.machine.transport;

import org.bukkit.block.BlockFace;

public interface PipeReceiver {
    public boolean canReceiveItem(MovingItem item, BlockFace input);
    public void addItem(MovingItem item, BlockFace input);
}
