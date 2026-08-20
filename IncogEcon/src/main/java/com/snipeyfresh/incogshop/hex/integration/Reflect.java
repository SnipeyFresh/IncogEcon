package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Small reflection helpers used by the Hex compatibility hooks.
 *
 * <p>IncogEcon never compiles against the plugins it integrates with, so every
 * lookup here is allowed to fail. A hook whose classes or methods are missing
 * simply reports itself as unavailable and the Hex falls back to its own
 * upgrade paths.</p>
 */
public final class Reflect {
    private Reflect() {}

    public static Class<?> findClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
                // Try the next candidate: plugin packages move between majors.
            }
        }
        return null;
    }

    public static Method method(Class<?> owner, String[] names, Class<?>... parameters) {
        if (owner == null) return null;
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // Keep looking; the same call is named differently across versions.
            }
        }
        return null;
    }

    /** Finds a method by name and parameter count when exact types are unknown. */
    public static Method looseMethod(Class<?> owner, String[] names, int parameterCount) {
        if (owner == null) return null;
        for (String name : names) {
            for (Method method : owner.getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != parameterCount) continue;
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    public static Object call(Method method, Object target, Object... arguments) {
        if (method == null) return null;
        try {
            return method.invoke(target, arguments);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object staticField(Class<?> owner, String... names) {
        if (owner == null) return null;
        for (String name : names) {
            try {
                Field field = owner.getField(name);
                field.setAccessible(true);
                return field.get(null);
            } catch (Throwable ignored) {
                // Not present in this version.
            }
        }
        return null;
    }

    /**
     * Calls a static plugin method whose first parameter is either an ItemStack
     * or an ItemMeta, which differs between versions of the same plugin. When a
     * meta is required it is written back to the stack so in-place edits stick.
     */
    public static Object invokeWithItem(Method method, ItemStack item, Object... extra) {
        if (method == null || item == null) return null;
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 0) return call(method, null);

        Object[] arguments = new Object[parameters.length];
        for (int i = 1; i < arguments.length; i++) arguments[i] = i - 1 < extra.length ? extra[i - 1] : null;

        if (parameters[0].isAssignableFrom(ItemStack.class)) {
            arguments[0] = item;
            return call(method, null, arguments);
        }
        if (ItemMeta.class.isAssignableFrom(parameters[0])) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;
            arguments[0] = meta;
            Object result = call(method, null, arguments);
            item.setItemMeta(meta);
            return result;
        }
        return null;
    }

    /** Reads a human-readable identifier from an unknown plugin object. */
    public static String nameOf(Object value) {
        if (value == null) return null;
        for (String candidate : new String[]{"getId", "getID", "getName", "getKey"}) {
            Object result = call(method(value.getClass(), new String[]{candidate}), value);
            if (result instanceof String text && !text.isBlank()) return text;
            if (result != null && !(result instanceof String)) {
                String text = String.valueOf(result);
                if (!text.isBlank()) return text;
            }
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
