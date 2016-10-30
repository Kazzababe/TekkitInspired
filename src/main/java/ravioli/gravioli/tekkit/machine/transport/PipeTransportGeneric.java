package ravioli.gravioli.tekkit.machine.transport;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.machines.MachineFilter;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.util.CommonUtils;

public class PipeTransportGeneric extends PipeTransport {
    private MovingItemSet getItems() {
        return this.container.items;
    }

    @Override
    public void addItem(MovingItem item, BlockFace input) {
        this.getItems().add(item);
        
        item.input = input;
        item.output = this.getDestination(item);

        Location start = this.container.getLocation().clone().add(0.5, 0.5, 0.5);
        start.add(input.getModX() * 0.5, input.getModY() * 0.5, input.getModZ() * 0.5);
        CommonUtils.normalizeLocation(start);

        item.moveTo(start);

        if (item.output == null) {
            item.output = input.getOppositeFace();
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        return this.getItems().stream().map(MovingItem::getItemStack).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void update() {
        Iterator iterator = this.getItems().iterator();
        while (iterator.hasNext()) {
            MovingItem item = (MovingItem) iterator.next();
            if (item.output == null) {
                continue;
            }
            Location mid = this.container.getLocation().clone().add(0.5, 0.5, 0.5);
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
            if ((this.endReached(item) || this.outOfBounds(item)) && item.reachedCenter) {
                iterator.remove();

                Block block = this.container.getBlock().getRelative(item.output);
                boolean success = this.injectItem(item, block);

                if (!success) {
                    item.destroy();
                    block.getWorld().dropItemNaturally(location, item.getItemStack());
                }
            }
        }
    }

    @Override
    public void destroy() {
        this.getItems().forEach(MovingItem::destroy);
    }

    private boolean injectItem(MovingItem item, Block block) {
        ItemStack itemStack = item.getItemStack();

        MachineBase machine = MachineManager.getMachineByLocation(block.getLocation());
        if (machine != null) {
            if (machine instanceof PipeReceiver) {
                PipeReceiver receiver = (PipeReceiver) machine;
                receiver.addItem(item, item.output.getOppositeFace());
                item.destroy();

                return true;
            } else if (machine instanceof Pipe) {
                this.passItem(item, (Pipe) machine);

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

    public void passItem(MovingItem item, Pipe pipe) {
        BlockFace input = item.output.getOppositeFace();
        item.reset();
        pipe.addItem(item, input);

    }

    public BlockFace getDestination(MovingItem item) {
        BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        ArrayList<BlockFace> result = new ArrayList<BlockFace>();

        for (BlockFace face : faces) {
            if (this.container.acceptableOutput(face) && face != item.input) {
                Block block = this.container.getBlock().getRelative(face);
                MachineBase machine = MachineManager.getMachineByLocation(block.getLocation());
                if (machine != null && machine instanceof MachineFilter && ((MachineFilter) machine).canReceiveItem(item, face.getOppositeFace())) {
                    result.add(face);
                }
            }
        }
        if (result.isEmpty()) {
            for (BlockFace face : faces) {
                if (this.container.acceptableOutput(face) && face != item.input) {
                    Block block = this.container.getBlock().getRelative(face);
                    if (item.canInsertItem(block, face.getOppositeFace())) {
                        result.add(face);
                    }
                }
            }
            if (result.isEmpty()) {
                for (BlockFace face : faces) {
                    if (this.container.acceptableOutput(face) && face != item.input) {
                        Block block = this.container.getBlock().getRelative(face);
                        MachineBase machine = MachineManager.getMachineByLocation(block.getLocation());
                        if (machine != null && machine instanceof Pipe && machine.acceptableInput(face.getOppositeFace())) {
                            result.add(face);
                        }
                    }
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        Collections.shuffle(result);

        return result.get(0);
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