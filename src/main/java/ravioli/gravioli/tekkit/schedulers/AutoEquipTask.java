package ravioli.gravioli.tekkit.schedulers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AutoEquipTask implements Runnable {
    private UUID uniqueId;
    private ItemStack toReplace;
    private EquipmentSlot hand;

    public AutoEquipTask(Player player, ItemStack toReplace, EquipmentSlot hand) {
        this.uniqueId = player.getUniqueId();
        this.toReplace = toReplace;
        this.hand = hand;
    }

    @Override
    public void run() {
        Player player = Bukkit.getPlayer(this.uniqueId);
        if (player != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    if (item.isSimilar(this.toReplace)) {
                        boolean empty = player.getInventory().getItemInMainHand().getType() == Material.AIR;
                        if (!empty) {
                            break;
                        }
                        player.getInventory().removeItem(item);
                        if (this.hand == EquipmentSlot.HAND) {
                            player.getInventory().setItemInMainHand(item);
                            break;
                        }
                        player.getInventory().setItemInOffHand(item);
                        break;
                    }
                }
            }
        }
    }
}