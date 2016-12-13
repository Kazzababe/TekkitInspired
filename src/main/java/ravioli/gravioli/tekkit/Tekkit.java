package ravioli.gravioli.tekkit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ravioli.gravioli.tekkit.commands.GiveMachineCommand;
import ravioli.gravioli.tekkit.database.Sqlite;
import ravioli.gravioli.tekkit.listeners.MachineListeners;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.serializers.InventorySerializer;
import ravioli.gravioli.tekkit.machines.serializers.LocationSerializer;
import ravioli.gravioli.tekkit.machines.serializers.UUIDSerializer;
import ravioli.gravioli.tekkit.machines.standard.*;
import ravioli.gravioli.tekkit.machines.transport.pipes.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.UUID;

public class Tekkit extends JavaPlugin {
    private Sqlite sqlite;

    public static boolean AUTO_EQUIP;

    @Override
    public void onLoad() {
        sqlite = new Sqlite(this);
    }

    @Override
    public void onEnable() {
        setupConfig();

        registerListener(new MachineListeners(this));
        registerCommand(new GiveMachineCommand());

        MachinesManager.addSerializer(Location.class, new LocationSerializer());
        MachinesManager.addSerializer(Inventory.class, new InventorySerializer());
        MachinesManager.addSerializer(UUID.class, new UUIDSerializer());
        MachinesManager.registerMachine(new MachineMiningWell(this));
        MachinesManager.registerMachine(new MachineBlockBreaker(this));
        MachinesManager.registerMachine(new MachineFilter(this));
        MachinesManager.registerMachine(new MachineTimer(this));
        MachinesManager.registerMachine(new PipeWooden(this));
        MachinesManager.registerMachine(new PipeGold(this));
        MachinesManager.registerMachine(new PipeIron(this));
        MachinesManager.registerMachine(new PipeDiamond(this));
        MachinesManager.registerMachine(new PipeVoid(this));
        MachinesManager.registerMachine(new MachineWorldAnchor(this));
        MachinesManager.registerMachine(new MachineCropomatic(this));
        MachinesManager.registerMachine(new MachineQuarry(this));

        MachinesManager.loadMachinesInWorld(this, Bukkit.getWorlds().get(0).getName());
    }

    @Override
    public void onDisable() {
        try {
            MachinesManager.getMachines().forEach(Machine::save);
            sqlite.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupConfig() {
        FileConfiguration config = this.getConfig();
        config.addDefault("auto-equip", true);
        config.options().copyDefaults(true);
        this.saveConfig();

        AUTO_EQUIP = config.getBoolean("auto-equip");
    }

    /**
     * Register all events in all the specified listener classes
     *
     * @param listeners Listeners to register
     */
    public void registerListener(Listener... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    /**
     * Dynamically register a command
     *
     * @param command The command to register
     */
    public void registerCommand(ravioli.gravioli.tekkit.commands.Command command) {
        CommandMap commandMap = null;
        try {
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(this.getServer().getPluginManager());
            commandMap.register(this.getName(), command);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the manager object that controls sqlite functions
     *
     * @return sqlite manager
     */
    public Sqlite getSqlite() {
        return sqlite;
    }
}
