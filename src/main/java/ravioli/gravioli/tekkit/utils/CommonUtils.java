package ravioli.gravioli.tekkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class CommonUtils {
    public static String locationToString(Location location) {
        return location.getWorld().getName() + "," +
                Double.valueOf(location.getX()) + "," +
                Double.valueOf(location.getY()) + "," +
                Double.valueOf(location.getZ());
    }

    public static Location stringToLocation(String string) {
        String[] info = string.split(",");
        return new Location(
                Bukkit.getWorld(info[0]),
                Double.parseDouble(info[1]),
                Double.parseDouble(info[2]),
                Double.parseDouble(info[3]));
    }

    public static Field[] getAllFields(Class clazz) {
        HashSet<Field> fields = new HashSet<Field>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        while (clazz.getSuperclass() != null) {
            clazz = clazz.getSuperclass();
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        return fields.toArray(new Field[fields.size()]);
    }

    public static BlockFace getCardinalDirection(Player player) {
        double rotation = (player.getLocation().getYaw() - 90.0) % 360.0;
        if (rotation < 0.0) {
            rotation += 360.0;
        }
        if (rotation < 22.5) {
            return BlockFace.WEST;
        }
        if (rotation < 112.5) {
            return BlockFace.NORTH;
        }
        if (rotation < 202.5) {
            return BlockFace.EAST;
        }
        if (rotation < 292.5) {
            return BlockFace.SOUTH;
        }
        if (rotation <= 360.0) {
            return BlockFace.WEST;
        }
        return null;
    }

    public static void minMaxCorners(Location c1, Location c2) {
        Vector min = new Vector(
                Math.min(c1.getBlockX(), c2.getBlockX()),
                Math.min(c1.getBlockY(), c2.getBlockY()),
                Math.min(c1.getBlockZ(), c2.getBlockZ()));
        Vector max = new Vector(
                Math.max(c1.getBlockX(), c2.getBlockX()),
                Math.max(c1.getBlockY(), c2.getBlockY()),
                Math.max(c1.getBlockZ(), c2.getBlockZ()));

        c1.setX(min.getX());
        c1.setY(min.getY());
        c1.setZ(min.getZ());

        c2.setX(max.getX());
        c2.setY(max.getY());
        c2.setZ(max.getZ());
    }

    public static ArrayList<Location> getPointsInRegion(Location c1, Location c2) {
        ArrayList<Location> result = new ArrayList();

        Location min = new Location(c1.getWorld(),
                Math.min(c1.getBlockX(), c2.getBlockX()),
                Math.min(c1.getBlockY(), c2.getBlockY()),
                Math.min(c1.getBlockZ(), c2.getBlockZ()));
        Location max = new Location(c1.getWorld(),
                Math.max(c1.getBlockX(), c2.getBlockX()),
                Math.max(c1.getBlockY(), c2.getBlockY()),
                Math.max(c1.getBlockZ(), c2.getBlockZ()));

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            result.add(new Location(c1.getWorld(), x, min.getBlockY(), min.getBlockZ()));
            result.add(new Location(c1.getWorld(), x, min.getBlockY(), max.getBlockZ()));
            result.add(new Location(c1.getWorld(), x, max.getBlockY(), min.getBlockZ()));
            result.add(new Location(c1.getWorld(), x, max.getBlockY(), max.getBlockZ()));
        }
        for (int y = min.getBlockY() + 1; y < max.getBlockY(); y++) {
            result.add(new Location(c1.getWorld(), min.getBlockX(), y, min.getBlockZ()));
            result.add(new Location(c1.getWorld(), min.getBlockX(), y, max.getBlockZ()));
            result.add(new Location(c1.getWorld(), max.getBlockX(), y, min.getBlockZ()));
            result.add(new Location(c1.getWorld(), max.getBlockX(), y, max.getBlockZ()));
        }
        for (int z = min.getBlockZ() + 1; z < max.getBlockZ(); z++) {
            result.add(new Location(c1.getWorld(), min.getBlockX(), min.getBlockY(), z));
            result.add(new Location(c1.getWorld(), min.getBlockX(), max.getBlockY(), z));
            result.add(new Location(c1.getWorld(), max.getBlockX(), min.getBlockY(), z));
            result.add(new Location(c1.getWorld(), max.getBlockX(), max.getBlockY(), z));
        }
        return result;
    }

    public static ArrayList<Location> getBlocksInCuboid(Location c1, Location c2) {
        ArrayList<Location> result = new ArrayList<Location>();

        Location min = new Location(c1.getWorld(),
                Math.min(c1.getBlockX(), c2.getBlockX()),
                Math.min(c1.getBlockY(), c2.getBlockY()),
                Math.min(c1.getBlockZ(), c2.getBlockZ()));
        Location max = new Location(c1.getWorld(),
                Math.max(c1.getBlockX(), c2.getBlockX()),
                Math.max(c1.getBlockY(), c2.getBlockY()),
                Math.max(c1.getBlockZ(), c2.getBlockZ()));

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    result.add(new Location(c1.getWorld(), x, y, z));
                }
            }
        }
        return result;
    }

    public static void normalizeLocation(Location location, double delim) {
        location.setX(Math.round(location.getX() * delim) / delim);
        location.setY(Math.round(location.getY() * delim) / delim);
        location.setZ(Math.round(location.getZ() * delim) / delim);
    }

    public static void normalizeLocation(Location location) {
        location.setX(Math.round(location.getX() * 20.0) / 20.0);
        location.setY(Math.round(location.getY() * 20.0) / 20.0);
        location.setZ(Math.round(location.getZ() * 20.0) / 20.0);
    }
}