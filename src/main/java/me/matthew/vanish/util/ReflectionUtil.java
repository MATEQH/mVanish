package me.matthew.vanish.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtil {

    /**
     * Post runnable on main thread
     *
     * @param runnable the runnable
     */
    public static void postToMainThread(Runnable runnable) {
        try {
            Class<?> clazz = getClass("MinecraftServer");
            Object instance = getMethod(clazz, "getServer", null);
            instance.getClass().getMethod("postToMainThread", Runnable.class).invoke(instance, runnable);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the class by name
     *
     * @param className the class name
     * @return the class
     */
    public static Class<?> getClass(String className) {
        try {
            return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the method by name
     *
     * @param clazz the class
     * @param methodName method name
     * @param object invoke object
     * @param args invoke args (object array)
     * @return method as object
     */
    public static Object getMethod(Class<?> clazz, String methodName, Object object, Object... args) {
        try {
            return clazz.getDeclaredMethod(methodName).invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the field by name
     *
     * @param clazz the class
     * @param fieldName field name
     * @return field as object
     */
    public static Object getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName).get(clazz);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Send packet to player
     *
     * Not tested, idk does it work
     *
     * @param player the player
     * @param packet the packet
     */
    public void sendPacket(Player player, Object packet) {
        try {
            Object nmsPlayer = getMethod(player.getClass(), "getHandle", null);
            Object playerConnection = getField(nmsPlayer.getClass(), "playerConnection");
            playerConnection.getClass().getMethod("sendPacket", getClass("Packet")).invoke(playerConnection, packet);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
