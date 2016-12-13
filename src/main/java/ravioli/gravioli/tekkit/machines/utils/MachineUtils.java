package ravioli.gravioli.tekkit.machines.utils;

import org.bukkit.Material;

public class MachineUtils {
    public static boolean isFuelSource(Material type) {
        switch (type) {
            case LAVA_BUCKET:
            case COAL_BLOCK:
            case BLAZE_ROD:
            case COAL:
            case BOAT:
            case WOOD:
            case LOG:
            case FENCE:
            case FENCE_GATE:
            case WOOD_STAIRS:
            case TRAP_DOOR:
            case WORKBENCH:
            case BOOKSHELF:
            case CHEST:
            case TRAPPED_CHEST:
            case DAYLIGHT_DETECTOR:
            case JUKEBOX:
            case NOTE_BLOCK:
            case BANNER:
            case WOOD_STEP:
            case BOW:
            case FISHING_ROD:
            case LADDER:
            case SIGN:
            case WOOD_DOOR:
            case BOWL:
            case SAPLING:
            case STICK:
            case WOOD_BUTTON:
            case WOOL:
            case CARPET:
            case WOOD_AXE:
            case WOOD_HOE:
            case WOOD_PICKAXE:
            case WOOD_SWORD:
                return true;
        }
        return false;
    }

    public static boolean isSmeltable(Material type) {
        switch (type) {
            case PORK:
            case RAW_BEEF:
            case RAW_CHICKEN:
            case RAW_FISH:
            case POTATO_ITEM:
            case MUTTON:
            case RABBIT:
            case IRON_ORE:
            case GOLD_ORE:
            case SAND:
            case COBBLESTONE:
            case CLAY:
            case NETHERRACK:
            case CLAY_BALL:
            case DIAMOND_ORE:
            case LAPIS_ORE:
            case REDSTONE_ORE:
            case COAL_ORE:
            case EMERALD_ORE:
            case QUARTZ_ORE:
            case LOG:
            case CACTUS:
            case CHORUS_FRUIT:
                return true;
        }
        return false;
    }
}
