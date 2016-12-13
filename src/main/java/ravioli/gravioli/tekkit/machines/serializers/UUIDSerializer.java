package ravioli.gravioli.tekkit.machines.serializers;

import java.util.UUID;

public class UUIDSerializer extends DatabaseSerializer<UUID> {
    @Override
    public String serialize(UUID object) {
        return object.toString();
    }

    @Override
    public UUID deserialize(String object) {
        return UUID.fromString(object);
    }
}
