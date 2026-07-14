package net.multyfora.client.integration.xaeros;

public record WaypointData(String name, String initials, int x, int y, int z, int color) {
    public double distanceTo(double bx, double by, double bz) {
        double dx = x - bx;
        double dy = y - by;
        double dz = z - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
