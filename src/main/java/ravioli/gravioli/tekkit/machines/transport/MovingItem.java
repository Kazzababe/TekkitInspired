package ravioli.gravioli.tekkit.machines.transport;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseSerializable;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.transport.utils.TransportUtils;
import ravioli.gravioli.tekkit.utils.CommonUtils;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MovingItem implements DatabaseSerializable {
    private ItemStack item;
    private Location location;
    private ArmorStand entity;

    public BlockFace input;
    public BlockFace output;
    public boolean reachedCenter;

    public MovingItem(ItemStack item, Location location, BlockFace input) {
        this.item = item;
        this.location = location.clone().add(0.5, 0.5, 0.5);
        this.location.add(0.5 * input.getModX(), 0.5 * input.getModY(), 0.5 * input.getModZ());
        this.input = input;

        entity = (ArmorStand) location.getWorld().spawnEntity(this.location.clone().add(0, item.getType().isSolid()? -0.88 : -1.18, 0), EntityType.ARMOR_STAND);
        entity.setGravity(false);
        entity.setCustomName(item != null ? item.getType().name() : "AIR");
        entity.setCustomNameVisible(false);
        entity.setVisible(false);
        entity.setMarker(true);
        entity.setCollidable(false);
        entity.setSmall(true);
        entity.setHelmet(item);
        entity.setBasePlate(false);
        entity.setMetadata("display", new FixedMetadataValue(Tekkit.getPlugin(Tekkit.class), this));
    }

    /**
     * Will move to the input location of the given block location
     *
     * @param location location of block
     */
    public void moveToInputStart(Location location) {
        Location start = location.clone().add(0.5, 0.5, 0.5);
        start.add(input.getModX() * 0.5, input.getModY() * 0.5, input.getModZ() * 0.5);
        CommonUtils.normalizeLocation(start);

        moveTo(start);
    }

    public void moveTo(Location location) {
        this.location = location;

        if (item.getType().isSolid()) {
            entity.teleport(location.clone().add(0, -0.88, 0));
        } else {
            entity.teleport(location.clone().add(0, -1.18, 0));
        }
    }

    public ItemStack getItemStack() {
        return item;
    }

    public void setItemStack(ItemStack item) {
        this.item = item;
        entity.setHelmet(item);
    }

    public Location getLocation() {
        return location;
    }

    public ArmorStand getEntity() {
        return entity;
    }

    public void destroy() {
        entity.remove();
        entity.setHealth(0);
    }

    public void reset() {
        reachedCenter = false;
        input = null;
        output = null;
    }

    public boolean canInsertItem(Block block, BlockFace input) {
        Machine machine = MachinesManager.getMachineByLocation(block.getLocation());
        if (machine != null) {
            if (machine instanceof TransportReceiver) {
                TransportReceiver receiver = (TransportReceiver) machine;
                return receiver.canReceiveItem(this, input);
            }
            return false;
        }

        if (block.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
            return TransportUtils.canInventoryBlockReceive(inventoryHolder, item, input);
        }
        return false;
    }

    @Override
    public String serialize() {
        Map<String, String> data = new HashMap();

        data.put("item", InventoryUtils.itemStackArrayToBase64(item));
        data.put("location", CommonUtils.locationToString(location));
        data.put("input", input.toString());
        data.put("output", output == null? "null" : output.toString());
        data.put("reachedCenter", reachedCenter + "");

        return new Gson().toJson(data);
    }

    public static MovingItem deserialize(String object) {
        Map<String, String> data = new Gson().fromJson(object, new TypeToken<Map<String, String>>(){}.getType());

        try {
            ItemStack item = InventoryUtils.itemStackArrayFromBase64(data.get("item"))[0];
            Location location = CommonUtils.stringToLocation(data.get("location"));
            BlockFace input = BlockFace.valueOf(data.get("input"));

            MovingItem movingItem = new MovingItem(item, location, input);
            movingItem.output = data.get("output").equals("null")? null : BlockFace.valueOf(data.get("output"));
            movingItem.reachedCenter = Boolean.getBoolean(data.get("reachedCenter"));

            return movingItem;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
