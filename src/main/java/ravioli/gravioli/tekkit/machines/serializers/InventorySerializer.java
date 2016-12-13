package ravioli.gravioli.tekkit.machines.serializers;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

import java.io.IOException;

public class InventorySerializer extends DatabaseSerializer<Inventory> {
    @Override
    public String serialize(Inventory object) {
        return object.getSize() + "|" + object.getTitle() + "|" + InventoryUtils.itemStackArrayToBase64(object.getContents());
    }

    @Override
    public Inventory deserialize(String object) {
        String[] data = object.split("\\|");

        Inventory inventory = Bukkit.createInventory(null, Integer.parseInt(data[0]), data[1]);
        try {
            inventory.setContents(InventoryUtils.itemStackArrayFromBase64(data[2]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inventory;
    }
}
