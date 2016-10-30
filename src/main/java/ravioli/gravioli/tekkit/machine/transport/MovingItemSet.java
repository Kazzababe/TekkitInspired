package ravioli.gravioli.tekkit.machine.transport;

import java.util.*;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.util.CommonUtils;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public class MovingItemSet extends AbstractSet<MovingItem> {
    private Set<MovingItem> items = Sets.newConcurrentHashSet();

    public void add(ItemStack item, Location location, BlockFace input) {
        MovingItem movingItem = new MovingItem(item, location, input);
        this.items.add(movingItem);
    }

    public Set<MovingItem> getItems() {
        return this.items;
    }

    @Override
    public Iterator<MovingItem> iterator() {
        return this.items.iterator();
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean add(MovingItem item) {
        return this.items.add(item);
    }

    @Override
    public boolean remove(Object object) {
        return this.items.remove(object);
    }

    public String toString() {
        String[] toSave = new String[this.items.size()];

        int count = 0;
        Iterator<MovingItem> iterator = this.iterator();
        while (iterator.hasNext()) {
            MovingItem item = iterator.next();
            String data = InventoryUtils.itemStackArrayToBase64(new ItemStack[] {item.getItemStack()}) + "|";
            data += CommonUtils.locationToString(item.getLocation()) + "|";
            data += item.input.toString() + "|";
            data += item.output.toString();
            toSave[count] = data;

            count++;
        }
        return StringUtils.join(toSave, ":");
    }
}