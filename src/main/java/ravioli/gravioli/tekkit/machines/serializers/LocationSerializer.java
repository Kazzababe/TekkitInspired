package ravioli.gravioli.tekkit.machines.serializers;

import org.bukkit.Location;
import ravioli.gravioli.tekkit.utils.CommonUtils;

public class LocationSerializer extends DatabaseSerializer<Location> {
    @Override
    public String serialize(Location object) {
        return CommonUtils.locationToString(object);
    }

    @Override
    public Location deserialize(String object) {
        return CommonUtils.stringToLocation(object);
    }
}
