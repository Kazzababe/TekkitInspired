package ravioli.gravioli.tekkit.machine.transport;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.util.CommonUtils;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public class MovingItemSet {
    private List<MovingItem> items = new ArrayList<MovingItem>();

    public void add(ItemStack item, Location location, BlockFace input) {
        MovingItem movingItem = new MovingItem(item, location, input);
        this.items.add(movingItem);
    }

    public void remove(MovingItem item) {
        this.items.remove(item);
    }

    public void remove(int index) {
        this.items.remove(index);
    }

    public MovingItem get(int index) {
        return this.items.get(index);
    }

    public List<MovingItem> getItems() {
        return this.items;
    }

    public String toString() {
        String[] toSave = new String[this.items.size()];
        for (int i = 0; i < this.items.size(); i++) {
            MovingItem item = this.items.get(i);
            String data = InventoryUtils.itemStackArrayToBase64(new ItemStack[] {item.getItem()}) + "|";
            data += CommonUtils.locationToString(item.getLocation()) + "|";
            data += item.input.toString() + "|";
            data += item.output.toString();
            toSave[i] = data;
        }
        return StringUtils.join(toSave, ":");
    }
}