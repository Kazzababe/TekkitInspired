package ravioli.gravioli.tekkit.machines.transport;

import org.bukkit.block.BlockFace;

public interface TransportReceiver {
    boolean canReceiveItem(MovingItem item, BlockFace input);
    void addMovingItem(MovingItem item, BlockFace input);
}
