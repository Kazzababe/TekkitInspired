package ravioli.gravioli.tekkit.machine.utils;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public class Fuel {
    public static Map<Material, Fuel> FUELS = new HashMap<Material, Fuel>();
    public static final Fuel COAL = new Fuel(Material.COAL, 80000);
    public static final Fuel COAL_BLOCK = new Fuel(Material.COAL_BLOCK, 800000);
    public static final Fuel LAVA_BUCKET = new Fuel(Material.LAVA_BUCKET, 1000000);
    public static final Fuel BLAZE_ROD = new Fuel(Material.BLAZE_ROD, 120000);

    private Material type;
    private long duration;

    public Fuel(Material type, long duration) {
        this.type = type;
        this.duration = duration;

        FUELS.put(type, this);
    }

    public Material getType() {
        return this.type;
    }

    public long getDuration() {
        return this.duration;
    }
 }


