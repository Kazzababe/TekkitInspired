package ravioli.gravioli.tekkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;

public class GiveMachineCommand extends Command {
    public GiveMachineCommand() {
        super("givemachine", "machine");
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        if (player.isOp()) {
            if (args.length == 1) {
                for (Machine machine : MachinesManager.registeredMachines) {
                    if (machine.getName().equalsIgnoreCase(args[0])) {
                        player.getInventory().addItem(machine.getRecipe().getResult());
                        player.sendMessage(ChatColor.GREEN + machine.getName() + " has been added to your inventory.");
                        break;
                    }
                }
            }
        }

        return true;
    }
}
