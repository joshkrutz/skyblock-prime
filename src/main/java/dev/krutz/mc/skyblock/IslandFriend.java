package dev.krutz.mc.skyblock;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.UUID;

/**
 * This class is used to store the friend data of an island, and is used to serialize and deserialize the friend data to and from JSON.
 */
public class IslandFriend {
    @Expose @SerializedName("friend") private UUID uuid;
    @Expose @SerializedName("permissions") private ArrayList<String> permissions;

    public IslandFriend(UUID friendUUID){
        this.uuid = friendUUID;
        this.permissions = new ArrayList<String>();
    }

    public IslandFriend(UUID friendUUID, ArrayList<String> permissions){
        this.uuid = friendUUID;
        this.permissions = permissions;
    }

    /**
     * Give a permission to your friend
     * @param permission
     * @return true if the permission was added, false if the permission was already present
     */
    public boolean addPermission(String permission){
        return this.permissions.add(permission);
    }

    /**
     * Revoke a permission from your friend
     * @param permission
     * @return true if the permission was removed, false if the permission was not present
     */
    public boolean removePermission(String permission){
        return this.permissions.remove(permission);
    }

    /**
     * Check if your friend has a permission
     * @param permission
     * @return true if your friend has the permission, false if your friend does not have the permission
     */
    public boolean hasPermission(String permission){
        return this.permissions.contains(permission);
    }

    /**
     * Get the UUID of your friend
     * @return the UUID of your friend
     */
    public UUID getUUID(){
        return this.uuid;
    }
}