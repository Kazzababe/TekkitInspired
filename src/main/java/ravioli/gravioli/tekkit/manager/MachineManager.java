package ravioli.gravioli.tekkit.manager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Recipe;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.Machine;
import ravioli.gravioli.tekkit.machine.MachineBase;

public class MachineManager {
    private static Set<MachineBase> registeredMachines = new HashSet<MachineBase>();
    private Set<MachineBase> machines = new HashSet<MachineBase>();

    public void addMachine(MachineBase machine) {
        this.machines.add(machine);
    }

    public void removeMachine(MachineBase machine) {
        this.machines.remove(machine);
    }

    public MachineBase getMachineByLocation(Location location) {
        for (MachineBase machine : this.machines) {
            MachineBase realMachine = machine;
            if (!realMachine.getLocation().equals(location)) continue;
            return realMachine;
        }
        return null;
    }

    public <T extends MachineBase> T getMachineByLocation(Location location, Class<T> type) {
        Optional<MachineBase> optional = this.machines.stream().filter(machine -> machine.getClass().isAssignableFrom(type) && machine.getLocation().equals(location)).findFirst();
        if (optional.isPresent()) {
            return (T) optional.get();
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

    public Set<MachineBase> getMachines() {
        return this.machines;
    }
}