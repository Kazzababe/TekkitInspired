package ravioli.gravioli.tekkit.manager;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;

public class MachineManager {
    private static Set<MachineBase> registeredMachines = new HashSet<MachineBase>();
    private static HashMap<Location, MachineBase> machines = new HashMap<Location, MachineBase>();

    public void addMachine(MachineBase machine) {
        machines.put(machine.getLocation(), machine);
    }

    public void removeMachine(MachineBase machine) {
        this.machines.remove(machine.getLocation());
    }

    public static boolean isMachine(Location location) {
        return machines.containsKey(location);
    }

    public static MachineBase getMachineByLocation(Location location) {
        return machines.get(location);
    }

    public static <T extends MachineBase> T getMachineByLocation(Location location, Class<T> type) {
        MachineBase machine = machines.get(location);
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