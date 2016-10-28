package ravioli.gravioli.tekkit.machine.transport;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import ravioli.gravioli.tekkit.Tekkit;

public class MovingItem {
    private ItemStack item;
    private Location location;
    private ArmorStand armorStand;
    public BlockFace input;
    public BlockFace output;
    public boolean reachedCenter = false;

    public MovingItem(ItemStack item, Location location, BlockFace input) {
        this.item = item;
        this.location = location.clone().add(0.5, 0.5, 0.5);
        this.location.add(0.5 * input.getModX(), 0.5 * input.getModY(), 0.5 * input.getModZ());
        this.input = input;

        this.armorStand = (ArmorStand) location.getWorld().spawnEntity(this.location.clone().add(0, item.getType().isSolid()? -0.88 : -1.18, 0), EntityType.ARMOR_STAND);
        this.armorStand.setGravity(false);
        this.armorStand.setCustomName(item != null ? item.getType().name() : "AIR");
        this.armorStand.setCustomNameVisible(false);
        this.armorStand.setVisible(false);
        this.armorStand.setMarker(true);
        this.armorStand.setCollidable(false);
        this.armorStand.setSmall(true);
        this.armorStand.setHelmet(item);
        this.armorStand.setBasePlate(false);
        this.armorStand.setMetadata("display", new FixedMetadataValue(Tekkit.getInstance(), this));

        this.moveTo(this.location);
    }

    public void moveTo(Location location) {
        this.location = location;

        // The below doesn't really appear to actually work
        if (this.item.getType().isSolid()) {
            this.armorStand.teleport(location.clone().add(0, -0.88, 0));
        } else {
            this.armorStand.teleport(location.clone().add(0, -1.13, 0));
        }
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        this.armorStand.setHelmet(item);
    }

    public Location getLocation() {
        return this.location;
    }

    public ArmorStand getArmorStand() {
        return this.armorStand;
    }

    public void destroy() {
        this.armorStand.remove();
        this.armorStand.setHealth(0);
    }
}