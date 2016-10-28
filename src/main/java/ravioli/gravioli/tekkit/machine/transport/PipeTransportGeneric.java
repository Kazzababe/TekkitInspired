package ravioli.gravioli.tekkit.machine.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.MachineWithInventory;
import ravioli.gravioli.tekkit.machine.machines.MachineFilter;
import ravioli.gravioli.tekkit.util.CommonUtils;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public class PipeTransportGeneric extends PipeTransport {
    private MovingItemSet getItems() {
        return this.container.items;
    }

    @Override
    public void addItem(MovingItem item, BlockFace input) {
        this.getItems().getItems().add(item);
        item.input = input;
        item.output = this.getDestination(item);

        Location start = this.container.getLocation().clone().add(0.5, 0.5, 0.5);
        start.add(input.getModX() * 0.5, input.getModY() * 0.5, input.getModZ() * 0.5);
        CommonUtils.normalizeLocation(start);
        item.moveTo(start);

        if (item.output == null) {
            item.getLocation().getWorld().dropItemNaturally(item.getLocation(), item.getItem());
            item.destroy();
            this.getItems().getItems().remove(item);
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        return this.getItems().getItems().stream().map(MovingItem::getItem).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void update() {
        Iterator iterator = this.getItems().getItems().iterator();
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
            if (this.endReached(item) || this.outOfBounds(item)) {
                CommonUtils.normalizeLocation(location, 2.0);
                item.moveTo(location);
                iterator.remove();

                Block block = this.container.getBlock().getRelative(item.output);
                MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(block.getLocation());
                if (machine != null && machine.acceptableInput(item.output.getOppositeFace())) {
                    if (machine instanceof Pipe) {
                        this.passItem(item, (Pipe) machine);
                        continue;
                    }
                    if (machine instanceof MachineWithInventory) {
                        HashMap<Integer, ItemStack> leftover = ((MachineWithInventory) machine).addItem(item.getItem(), item.output.getOppositeFace());
                        leftover.values().forEach(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
                        item.destroy();
                        continue;
                    }
                }
                if (block.getState() instanceof InventoryHolder) {
                    InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
                    HashMap<Integer, ItemStack> leftover = inventoryHolder.getInventory().addItem(item.getItem());
                    leftover.values().forEach(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
                    item.destroy();
                    continue;
                }
                item.destroy();
                block.getWorld().dropItemNaturally(location, item.getItem());
            }
        }
    }

    @Override
    public void destroy() {
        this.getItems().getItems().forEach(MovingItem::destroy);
    }

    public void passItem(MovingItem item, Pipe pipe) {
        item.reachedCenter = false;
        pipe.addItem(item, item.output.getOppositeFace());
    }

    public BlockFace getDestination(MovingItem item) {
        BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        ArrayList<BlockFace> result = new ArrayList<BlockFace>();

        for (BlockFace face : faces) {
            if (this.container.acceptableOutput(face) && face != item.input) {
                Block block = this.container.getBlock().getRelative(face);
                MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(block.getLocation());
                if (machine != null && machine.acceptableInput(face.getOppositeFace()) && machine instanceof MachineFilter && ((MachineFilter) machine).canTransport(item.getItem())) {
                    result.add(face);
                }
            }
        }
        if (result.isEmpty()) {
            for (BlockFace face : faces) {
                if (this.container.acceptableOutput(face) && face != item.input) {
                    Block block = this.container.getBlock().getRelative(face);
                    MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(block.getLocation());
                    if (machine == null && block.getState() instanceof InventoryHolder) {
                        if (InventoryUtils.canFitIntoInventory(((InventoryHolder) block.getState()).getInventory(), item.getItem())) {
                            result.add(face);
                        }
                    }
                }
            }
            for (BlockFace face : faces) {
                if (this.container.acceptableOutput(face) && face != item.input) {
                    Block block = this.container.getBlock().getRelative(face);
                    MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(block.getLocation());
                    if (machine != null && machine.acceptableInput(face.getOppositeFace()) && machine instanceof MachineWithInventory && !(machine instanceof MachineFilter)) {
                        MachineWithInventory inventoryMachine = (MachineWithInventory) machine;
                        Inventory inventory = Bukkit.createInventory(null, inventoryMachine.getInventory().getSize());
                        inventory.setContents(inventoryMachine.getInventory().getContents());
                        if (inventory.addItem(item.getItem()).isEmpty()) {
                            result.add(face);
                        }
                        inventory = null;
                    }
                }
            }
            if (result.isEmpty()) {
                for (BlockFace face : faces) {
                    if (this.container.acceptableOutput(face) && face != item.input) {
                        Block block = this.container.getBlock().getRelative(face);
                        MachineBase machine = Tekkit.getInstance().getMachineManager().getMachineByLocation(block.getLocation());
                        if (machine != null && machine instanceof Pipe && machine.acceptableInput(face.getOppositeFace())) {
                            result.add(face);
                        }
                    }
                }
            }
        }
        Collections.shuffle(result);
        if (result.isEmpty()) {
            return null;
        }
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
        return item.getLocation().distance(mid) > 0.5;
    }
}