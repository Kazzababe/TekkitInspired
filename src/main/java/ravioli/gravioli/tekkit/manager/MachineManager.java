package ravioli.gravioli.tekkit.manager;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;

public class MachineManager {
    private static Set<MachineBase> registeredMachines = new HashSet<MachineBase>();
    private HashMap<Location, MachineBase> machines = new HashMap<Location, MachineBase>();

    public void addMachine(MachineBase machine) {
        this.machines.put(machine.getLocation(), machine);
    }

    public void removeMachine(MachineBase machine) {
        this.machines.remove(machine);
    }

    public boolean isMachine(Location location) {
        return this.machines.containsKey(location);
    }

    public MachineBase getMachineByLocation(Location location) {
        return this.machines.get(location);
    }

    public <T extends MachineBase> T getMachineByLocation(Location location, Class<T> type) {
        MachineBase machine = this.machines.get(location);
        if (machine != null && machine.getClass().isAssignableFrom(type)) {
            return (T) machine;
        }
        return null;
    }

    public static void registerMachine(MachineBase machine) {
        registeredMachines.add(machine);
        Bukkit.addRecipe(machine.getRecipe());
        Tekkit.getInstance().getSqlite().createTable(machine);
        Tekkit.getInstance().getSqlite().loadMachines(machine);
    }

    public static Set<MachineBase> getRegisteredMachines() {
        return registeredMachines;
    }

    public Collection<MachineBase> getMachines() {
        return this.machines.values();
    }
}