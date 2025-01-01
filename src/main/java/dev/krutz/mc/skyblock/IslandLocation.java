package dev.krutz.mc.skyblock;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * This class is used to store the location of an island, and is used to serialize and deserialize the location to and from JSON.
 */ 
public class IslandLocation {
    @Expose @SerializedName("spawn_world") private String world;
    @Expose @SerializedName("spawn_x") private double x;
    @Expose @SerializedName("spawn_y") private double y;
    @Expose @SerializedName("spawn_z") private double z;
    @Expose @SerializedName("spawn_yaw") private float yaw;
    @Expose @SerializedName("spawn_pitch") private float pitch;

    public IslandLocation(Location location){
        world = location.getWorld().getName();
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        yaw = location.getYaw();
        pitch = location.getPitch();
    }

    /**
     * Get the location object for this island location
     * @return
     */
    public Location getLocation(){
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    /**
     * Get the world object for this island location
     * @return
     */
    public World getWorld(){
        return Bukkit.getWorld(world);
    }
}