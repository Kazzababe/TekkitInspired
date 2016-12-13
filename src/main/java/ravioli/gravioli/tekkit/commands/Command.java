package ravioli.gravioli.tekkit.commands;

import org.bukkit.command.CommandSender;

import java.util.Arrays;

public abstract class Command extends org.bukkit.command.Command {
    public Command(String name, String... aliases) {
        super(name);
        this.setAliases(Arrays.asList(aliases));
    }
    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        return this.onCommand(commandSender, strings);
    }

    public abstract boolean onCommand(CommandSender sender, String[] args);
}