package fts.spi;

import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Loc3D implements Serializable {
    public final int x, y, z;
    public final String world;

    public Loc3D(String world, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public static @NotNull
    Loc3D from(@NotNull Location location) {
        World world = location.getWorld();
        String worldName = world == null ? null : world.getName();
        return new Loc3D(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Loc3D loc3D = (Loc3D) o;

        if (x != loc3D.x) {
            return false;
        }
        if (y != loc3D.y) {
            return false;
        }
        if (z != loc3D.z) {
            return false;
        }
        return Objects.equals(world, loc3D.world);
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        result = 31 * result + (world != null ? world.hashCode() : 0);
        return result;
    }

    public Location toLocation() {
        World world = this.world == null ? null : Bukkit.getWorld(this.world);
        return new Location(world, x, y, z);
    }

    public Location toUsableLocation() {
        Location loc = toLocation();
        if (loc.getWorld() == null) {
            throw new NoSuchElementException("No world `" + world + "` found.");
        }
        return loc;
    }
}

