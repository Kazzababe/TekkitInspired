package ravioli.gravioli.tekkit.machines;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.serializers.DatabaseSerializer;

import java.util.*;

public class MachinesManager {
    public static Set<Machine> registeredMachines = new HashSet();
    public static Map<String, Collection<Machine>> unloadedMachines = new HashMap();
    public static Map<String, Collection<Machine>> markedForDelete = new HashMap();
    private static Map<Location, Machine> machines = new HashMap();

    private static Map<Class, DatabaseSerializer> serializers = new HashMap();

    public static boolean addSerializer(Class clazz, DatabaseSerializer serializer) {
        if (!hasSerializer(clazz)) {
            serializers.put(clazz, serializer);
            return true;
        }
        return false;
    }

    public static boolean hasSerializer(Class clazz) {
        return serializers.containsKey(clazz);
    }

    public static DatabaseSerializer getSerializer(Class clazz) {
        return serializers.get(clazz);
    }

    public static Collection<Machine> getMachines() {
        return machines.values();
    }

    /**
     * Adds a machine to the world
     *
     * @param machine machine to add
     */
    public static void addMachine(Machine machine) {
        if (!machines.containsKey(machine.getLocation())) {
            machines.put(machine.getLocation(), machine);
        }
    }

    /**
     * Remove a machine from the world
     *
     * @param machine machine to remove
     */
    public static void removeMachine(Machine machine) {
        machines.remove(machine.getLocation());
    }

    /**
     * Checks whether or not the given location has a machine at it
     *
     * @param location location to check
     * @return if the location is a machine or not
     */
    public static boolean isMachine(Location location) {
        return machines.containsKey(location);
    }

    /**
     * Gets the machine at a specified location, but only if it's loaded
     *
     * @param location location to check
     * @return the machine at the given location
     */
    public static Machine getMachineByLocation(Location location) {
        Machine machine = machines.get(location);
        if (machine != null && machine.isLoaded()) {
            return machine;
        }
        return null;
    }

    /**
     * Loads all machines in the given world
     *
     * @param plugin tekkit plugin
     * @param worldName world name
     */
    public static void loadMachinesInWorld(Tekkit plugin, String worldName) {
        if (unloadedMachines.containsKey(worldName)) {
            int machinesLoaded = 0;
            long start = System.currentTimeMillis();
            for (Machine machine : unloadedMachines.get(worldName)) {
                if (!machine.isLoaded()) {
                    plugin.getSqlite().loadMachine(machine);
                    machinesLoaded++;
                }
            }
            unloadedMachines.remove(worldName);
            System.out.println("[Tekkit Inspired] Loaded " + machinesLoaded + " machines in '" + worldName + "' in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * Deletes all machines marked for deletion in the given world
     *
     * @param plugin tekkit plugin
     * @param worldName world name
     */
    public static void deleteMachinesInWorld(Tekkit plugin, String worldName) {
        if (markedForDelete.containsKey(worldName)) {
            int machinesDeleted = 0;
            long start = System.currentTimeMillis();
            for (Machine machine : markedForDelete.get(worldName)) {
                machine.delete();
                machinesDeleted++;
            }
            markedForDelete.remove(worldName);
            System.out.println("[Tekkit Inspired] Deleted " + machinesDeleted + " machines in '" + worldName + "' in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * Registers a machine so that players may interact with that machine type
     *
     * @param machine an instance of the machine to be registered
     */
    public static void registerMachine(Machine machine) {
        Bukkit.addRecipe(machine.getRecipe());

        registeredMachines.add(machine);
        machine.getPlugin().getSqlite().createTable(machine);
        machine.getPlugin().getSqlite().preloadMachines(machine);
    }
}
