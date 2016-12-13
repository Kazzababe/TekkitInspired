package ravioli.gravioli.tekkit.machines.transport;

import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.transport.pipes.Pipe;
import ravioli.gravioli.tekkit.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PipeTransportGeneric extends PipeTransport {
    private List<BlockFace> faces = new ArrayList(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

    @Override
    public void addItem(MovingItem item, BlockFace input) {
        getItemSet().add(item);

        item.input = input;
        item.output = getDestination(item);
        item.moveToInputStart(container.getLocation());

        if (item.output == null) {
            item.output = input.getOppositeFace();
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        return getItemSet().stream().map(MovingItem::getItemStack).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void update() {
        Iterator iterator = getItemSet().iterator();
        while (iterator.hasNext()) {
            MovingItem item = (MovingItem) iterator.next();
            if (item.output == null) {
                continue;
            }
            Location mid = container.getLocation().clone().add(0.5, 0.5, 0.5);
            CommonUtils.normalizeLocation(mid, 2.0);

            Location destination = mid.clone();
            destination.add(item.output.getModX() * 0.5, item.output.getModY() * 0.5, item.output.getModZ() * 0.5);
            CommonUtils.normalizeLocation(destination);

            double factor = this.container.getSpeed();
            Location loc = item.getLocation().clone();
            Location location = !item.reachedCenter ?
                    loc.add(item.input.getModX() * -factor, item.input.getModY() * -factor, item.input.getModZ() * -factor) :
                    loc.add(item.output.getModX() * factor, item.output.getModY() * factor, item.output.getModZ() * factor);
            CommonUtils.normalizeLocation(location);

            item.moveTo(location);
            if (mid.distance(location) < 0.1 && !item.reachedCenter) {
                item.reachedCenter = true;
                CommonUtils.normalizeLocation(location, 2.0);
            }
            if ((endReached(item) || outOfBounds(item)) && item.reachedCenter) {
                iterator.remove();

                Block block = container.getBlock().getRelative(item.output);
                boolean success = injectItem(item, block);

                if (!success) {
                    item.destroy();
                    block.getWorld().dropItemNaturally(location, item.getItemStack());
                }
            }
        }
    }

    @Override
    public void destroy() {
        getItemSet().forEach(MovingItem::destroy);
    }

    private boolean injectItem(MovingItem item, Block block) {
        ItemStack itemStack = item.getItemStack();

        Machine machine = MachinesManager.getMachineByLocation(block.getLocation());
        if (machine != null) {
            if (machine instanceof Pipe) {
                passItem(item, (Pipe) machine);
                return true;
            } else if (machine instanceof TransportReceiver) {
                TransportReceiver receiver = (TransportReceiver) machine;
                receiver.addMovingItem(item, item.output.getOppositeFace());
                item.destroy();
                return true;
            }
            return false;
        }

        if (block.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryBlock = (InventoryHolder) block.getState();

            if (inventoryBlock instanceof Hopper || inventoryBlock instanceof BrewingStand || inventoryBlock instanceof Beacon) {
                return false;
            }

            if (inventoryBlock instanceof Furnace) {
                Furnace furnace = (Furnace) inventoryBlock;

                ItemStack furnaceItem = null;
                Boolean smelting = null;

                if (item.output == BlockFace.UP) {
                    furnaceItem = furnace.getInventory().getFuel();
                    smelting = false;
                } else if (item.output == BlockFace.DOWN) {
                    furnaceItem = furnace.getInventory().getSmelting();
                    smelting = true;
                }
                if (furnaceItem != null) {
                    if (furnaceItem.isSimilar(itemStack)) {
                        int total = furnaceItem.getAmount() + itemStack.getAmount();
                        if (total <= furnaceItem.getMaxStackSize()) {
                            furnaceItem.setAmount(total);
                            item.destroy();
                            return true;
                        }
                        furnaceItem.setAmount(furnaceItem.getMaxStackSize());
                        itemStack.setAmount(total - furnaceItem.getMaxStackSize());
                        return false;
                    }
                } else if (smelting != null) {
                    if (smelting) {
                        furnace.getInventory().setSmelting(itemStack);
                    } else {
                        furnace.getInventory().setFuel(itemStack);
                    }
                    item.destroy();
                    return true;
                }
            } else {
                ItemStack leftover = inventoryBlock.getInventory().addItem(itemStack).get(0);
                if (leftover == null) {
                    item.destroy();
                    return true;
                }
                itemStack.setAmount(leftover.getAmount());
            }
        }
        return false;
    }

    private void passItem(MovingItem item, Pipe pipe) {
        BlockFace input = item.output.getOppositeFace();
        item.reset();
        pipe.addItem(item, input);
    }

    public BlockFace getDestination(MovingItem item) {
        List<BlockFace> results = new ArrayList();

        for (BlockFace face : faces) {
            if (container.acceptableOutput(face) && face != item.input) {
                Block block = container.getBlock().getRelative(face);
                if (item.canInsertItem(block, face.getOppositeFace())) {
                    results.add(face);
                }
            }
        }
        if (results.isEmpty()) {
            return null;
        }
        BlockFace result = results.get(0);
        Collections.rotate(faces, -1 - this.faces.indexOf(result));

        return result;
    }

    public MovingItemSet getItemSet() {
        return container.getItemSet();
    }

    private boolean endReached(MovingItem item) {
        Location destination = this.container.getLocation().clone().add(0.5, 0.5, 0.5);
        destination.add(item.output.getModX() * 0.5, item.output.getModY() * 0.5, item.output.getModZ() * 0.5);
        CommonUtils.normalizeLocation(destination);
        return item.getLocation().distance(destination) < 0.01;
    }

    private boolean outOfBounds(MovingItem item) {
        Location mid = this.container.getLocation().clone().add(0.5, 0.5, 0.5);
        CommonUtils.normalizeLocation(mid, 2.0);
        return item.getLocation().distance(mid) > 0.6;
    }
}
