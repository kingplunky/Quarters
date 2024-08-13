package net.earthmc.quarters.object.entity;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.earthmc.quarters.api.manager.ConfigManager;
import net.earthmc.quarters.api.manager.QuarterManager;
import net.earthmc.quarters.api.manager.TownMetadataManager;
import net.earthmc.quarters.object.wrapper.QuarterPermissions;
import net.earthmc.quarters.object.state.QuarterType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Quarter {

    private final Town town;
    private List<Cuboid> cuboids;
    private final UUID uuid = UUID.randomUUID();
    private final Long registered = System.currentTimeMillis();
    private UUID owner;
    private List<UUID> trusted = new ArrayList<>();
    private Double price = null;
    private QuarterType type = QuarterType.APARTMENT;
    private boolean isEmbassy = false;
    private Long claimedAt;
    private Color colour = ConfigManager.hasDefaultQuarterColour() ? ConfigManager.getDefaultQuarterColour() : getRandomColour();
    private String name = "Quarter";
    private final QuarterPermissions permissions = new QuarterPermissions(this);
    private Location anchor = null;

    public Quarter(Town town, List<Cuboid> cuboids) {
        this.town = town;
        this.cuboids = cuboids;
    }

    /**
     * This method must be called to save the quarter's instance to metadata after any change
     * The only exception is when using the {@link #delete()} method, that will save itself
     */
    public void save() {
        TownMetadataManager tmm = TownMetadataManager.getInstance();

        List<Quarter> quarters = QuarterManager.getInstance().getQuarters(town);
        quarters.remove(this);
        quarters.add(this);

        tmm.setQuarterList(town, quarters);
    }

    /**
     * Permanently delete this quarter from the town's metadata
     */
    public void delete() {
        TownMetadataManager tmm = TownMetadataManager.getInstance();

        List<Quarter> quarters = tmm.getQuarterList(town);
        quarters.remove(this);

        tmm.setQuarterList(town, quarters);
    }

    /**
     * @return An int representing the total number of blocks inside this quarter
     */
    public int getVolume() {
        int volume = 0;

        for (Cuboid cuboid : cuboids) {
            volume = volume + cuboid.getVolume();
        }

        return volume;
    }

    /**
     * @return True if the specified player is in this quarter's town
     */
    public boolean isPlayerInTown(@NotNull Player player) {
        return town.hasResident(player);
    }

    /**
     * @return True if the specified location is within this quarter's {@link org.bukkit.util.BoundingBox}
     */
    public boolean isLocationInsideBounds(@NotNull Location location) {
        for (Cuboid cuboid : cuboids) {
            if (cuboid.isLocationInsideBounds(location)) return true;
        }

        return false;
    }

    /**
     * @return True if the specified player owns this quarter or has {@link #hasLandlordPermissions(Player) landlord} permissions
     */
    public boolean hasBasicCommandPermissions(@NotNull Player player) { // TODO: consider if this will just be used for commands, change name if not
        UUID uuid = player.getUniqueId();

        if (owner != null && owner.equals(uuid)) return true;

        return hasLandlordPermissions(player);
    }

    /**
     * @return True if the specified player is in this quarter's town and is a mayor or has the landlord permission node
     */
    public boolean hasLandlordPermissions(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        if (town.getMayor().getUUID().equals(uuid)) return true;

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (!town.hasResident(resident)) return false;

        return player.hasPermission("quarters.landlord");
    }

    private Color getRandomColour() {
        Random random = new Random();
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    public boolean isForSale() {
        return price != null;
    }

    public @NotNull Location getFirstCornerOfFirstCuboid() {
        return cuboids.get(0).getCornerOne();
    }

    public @Nullable Nation getNation() {
        return town.getNationOrNull();
    }

    /**
     * @return The town that this quarter is located in
     */
    public @NotNull Town getTown() {
        return town;
    }

    public void setCuboids(List<Cuboid> cuboids) {
        this.cuboids = cuboids;
    }

    /**
     * @return A list of the cuboids within this quarter
     */
    public List<Cuboid> getCuboids() {
        return this.cuboids;
    }

    /**
     * @return A unique ID for this quarter, this will not change across restarts
     */
    public @NotNull UUID getUUID() {
        return uuid;
    }

    /**
     * @return A Long representing when the quarter was created
     */
    public long getRegistered() {
        return registered;
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
        if (owner == null) this.claimedAt = null;
    }

    /**
     * @return The quarter owner's UUID
     */
    public @Nullable UUID getOwner() {
        return owner;
    }

    /**
     * @return Gets the quarter owner as a {@link Resident}, if you want to change the owner use the setOwner() method that takes in a UUID
     */
    public @Nullable Resident getOwnerResident() {
        if (owner == null) return null;
        return TownyAPI.getInstance().getResident(uuid);
    }

    public void setTrusted(@NotNull List<UUID> trusted) {
        this.trusted = trusted;
    }

    /**
     * @return A list of trusted residents' UUIDs
     */
    public List<UUID> getTrusted() {
        return trusted;
    }

    /**
     * @return A list of all the trusted residents in this quarter
     */
    public List<Resident> getTrustedResidents() {
        List<Resident> trustedResidents = new ArrayList<>();

        for (UUID trustedUUID : getTrusted()) {
            trustedResidents.add(TownyAPI.getInstance().getResident(trustedUUID));
        }

        return trustedResidents;
    }

    public void setPrice(@Nullable Double price) {
        this.price = price;
    }

    /**
     * @return The current sale price or null if it is not for sale
     */
    public @Nullable Double getPrice() {
        return price;
    }

    public void setType(@NotNull QuarterType type) {
        this.type = type;
    }

    /**
     * @return The quarter's {@link QuarterType}
     */
    public QuarterType getType() {
        return type;
    }

    /**
     * Changing this to false will remove the current owner if they are not part of the quarter's town
     *
     * @param isEmbassy A boolean representing the state you want the embassy status to be in
     */
    public void setEmbassy(boolean isEmbassy) {
        this.isEmbassy = isEmbassy;

        Resident owner = getOwnerResident();
        if (owner != null && owner.getTownOrNull() != town) setOwner(null);
    }

    /**
     * Gets this quarter's embassy status
     * As opposed to how Towny handles embassies, embassy is not its own quarter type, it is a separate flag
     * Being an embassy allows for certain conditions outside just ownership when not in the quarter's town
     * for example, quarters of the station or common type will have their functionality extended to non-residents
     *
     * @return A boolean representing whether the quarter is an embassy
     */
    public boolean isEmbassy() {
        return isEmbassy;
    }

    public void setClaimedAt(@Nullable Long claimedAt) {
        this.claimedAt = claimedAt;
    }

    /**
     * @return A Long representing when ownership of a quarter last changed
     */
    public @Nullable Long getClaimedAt() {
        return claimedAt;
    }

    public void setColour(@NotNull Color colour) {
        this.colour = colour;
    }

    public @NotNull Color getColour() {
        return colour;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull QuarterPermissions getPermissions() {
        return permissions;
    }

    public void setAnchor(Location anchor) {
        this.anchor = anchor;
    }

    public @Nullable Location getAnchor() {
        return anchor;
    }
}
