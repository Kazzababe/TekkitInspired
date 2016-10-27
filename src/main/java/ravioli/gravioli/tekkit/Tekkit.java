package ravioli.gravioli.tekkit;

import java.sql.SQLException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ravioli.gravioli.tekkit.listeners.MachineListeners;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.machines.MachineBlockBreaker;
import ravioli.gravioli.tekkit.machine.machines.MachineFilter;
import ravioli.gravioli.tekkit.machine.machines.MachineMiningWell;
import ravioli.gravioli.tekkit.machine.machines.MachineQuarry;
import ravioli.gravioli.tekkit.machine.machines.MachineTimer;
import ravioli.gravioli.tekkit.machine.transport.pipes.PipeDiamond;
import ravioli.gravioli.tekkit.machine.transport.pipes.PipeGold;
import ravioli.gravioli.tekkit.machine.transport.pipes.PipeIron;
import ravioli.gravioli.tekkit.machine.transport.pipes.PipeSimple;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.storage.Sqlite;

public class Tekkit extends JavaPlugin {
    private static Tekkit instance;
    private static MachineManager machineManager;

    private Sqlite sqlite;
    public static boolean AUTO_EQUIP;

    @Override
    public void onLoad() {
        machineManager = new MachineManager();
        this.sqlite = new Sqlite(this);
    }

    @Override
    public void onEnable() {
        instance = this;
        this.setupConfig();

        MachineManager.registerMachine(new MachineBlockBreaker());
        MachineManager.registerMachine(new MachineMiningWell());
        MachineManager.registerMachine(new MachineTimer());
        MachineManager.registerMachine(new MachineFilter());
        MachineManager.registerMachine(new PipeSimple());
        MachineManager.registerMachine(new PipeIron());
        MachineManager.registerMachine(new PipeGold());
        MachineManager.registerMachine(new PipeDiamond());
        MachineManager.registerMachine(new MachineQuarry());

        this.getServer().getPluginManager().registerEvents(new MachineListeners(this), this);
    }

    @Override
    public void onDisable() {
        try {
            machineManager.getMachines().forEach(MachineBase::save);
            this.sqlite.getConnection().close();
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

    public static Tekkit getInstance() {
        return instance;
    }

    public static MachineManager getMachineManager() {
        return machineManager;
    }

    public Sqlite getSqlite() {
        return this.sqlite;
    }


}