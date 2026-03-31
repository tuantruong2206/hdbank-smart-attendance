package com.hdbank.common.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility class for checking permissions against the RBAC permission matrix.
 * <p>
 * Permissions can be loaded from the database at startup (via {@link #loadPermissions})
 * or checked against the built-in static rules as a fallback when DB access is unavailable.
 * <p>
 * Thread-safe: uses ConcurrentHashMap for the in-memory cache.
 */
public final class PermissionChecker {

    private PermissionChecker() {
        // utility class
    }

    /**
     * Represents a single permission entry.
     */
    public record Permission(String role, String action, String resource, String scope) {
    }

    /**
     * In-memory cache: role -> set of "action:resource" keys.
     */
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new ConcurrentHashMap<>();

    /**
     * In-memory cache: "role:action:resource" -> scope.
     */
    private static final Map<String, String> PERMISSION_SCOPES = new ConcurrentHashMap<>();

    /**
     * Whether permissions have been loaded from DB.
     */
    private static volatile boolean loaded = false;

    /**
     * Load permissions from an external source (e.g., database query result).
     * Call this once at application startup.
     *
     * @param permissions list of permission entries from DB
     */
    public static void loadPermissions(List<Permission> permissions) {
        ROLE_PERMISSIONS.clear();
        PERMISSION_SCOPES.clear();

        for (Permission p : permissions) {
            String key = p.action() + ":" + p.resource();
            ROLE_PERMISSIONS.computeIfAbsent(p.role(), k -> ConcurrentHashMap.newKeySet()).add(key);
            PERMISSION_SCOPES.put(p.role() + ":" + key, p.scope());
        }

        loaded = true;
    }

    /**
     * Check if a role has permission for a given action on a resource.
     *
     * @param role     the user's role (e.g., "EMPLOYEE", "SYSTEM_ADMIN")
     * @param action   the action (e.g., "view", "create", "approve", "delete", "export", "lock", "edit")
     * @param resource the resource (e.g., "attendance", "timesheet", "leave", "admin", "report", "employee", "organization", "notification")
     * @return true if the role has the specified permission
     */
    public static boolean hasPermission(String role, String action, String resource) {
        if (role == null || action == null || resource == null) {
            return false;
        }

        if (loaded) {
            Set<String> perms = ROLE_PERMISSIONS.get(role);
            return perms != null && perms.contains(action + ":" + resource);
        }

        // Fallback: static rules when DB permissions not loaded
        return hasPermissionStatic(role, action, resource);
    }

    /**
     * Get the scope for a specific permission.
     *
     * @param role     the user's role
     * @param action   the action
     * @param resource the resource
     * @return the scope string (e.g., "self", "unit", "department", "all"), or null if no permission
     */
    public static String getScope(String role, String action, String resource) {
        if (role == null || action == null || resource == null) {
            return null;
        }

        if (loaded) {
            return PERMISSION_SCOPES.get(role + ":" + action + ":" + resource);
        }

        // Fallback: derive from static rules
        if (!hasPermissionStatic(role, action, resource)) {
            return null;
        }
        return resolveScopeStatic(role);
    }

    /**
     * Get all permissions for a given role.
     *
     * @param role the role name
     * @return list of permission entries for the role
     */
    public static List<Permission> getPermissionsForRole(String role) {
        if (role == null) {
            return List.of();
        }

        if (loaded) {
            Set<String> perms = ROLE_PERMISSIONS.get(role);
            if (perms == null) {
                return List.of();
            }
            List<Permission> result = new ArrayList<>();
            for (String key : perms) {
                String[] parts = key.split(":", 2);
                String scope = PERMISSION_SCOPES.get(role + ":" + key);
                result.add(new Permission(role, parts[0], parts[1], scope));
            }
            return result;
        }

        // Fallback not supported for listing all permissions
        return List.of();
    }

    /**
     * Check if permissions have been loaded from DB.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Clear all cached permissions. Useful for testing or forced reload.
     */
    public static void clear() {
        ROLE_PERMISSIONS.clear();
        PERMISSION_SCOPES.clear();
        loaded = false;
    }

    // ---- Static fallback rules (used when DB is unavailable) ----

    private static boolean hasPermissionStatic(String role, String action, String resource) {
        return switch (role) {
            case "SYSTEM_ADMIN" -> true; // full access
            case "CEO" -> isAllowedForCeo(action, resource);
            case "DIVISION_DIRECTOR" -> isAllowedForDirector(action, resource);
            case "REGION_DIRECTOR" -> isAllowedForRegionDirector(action, resource);
            case "DEPT_HEAD" -> isAllowedForDeptHead(action, resource);
            case "DEPUTY_HEAD" -> isAllowedForDeputyHead(action, resource);
            case "UNIT_HEAD" -> isAllowedForUnitHead(action, resource);
            case "EMPLOYEE" -> isAllowedForEmployee(action, resource);
            default -> false;
        };
    }

    private static boolean isAllowedForCeo(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "employee", "admin").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            case "export" -> "report".equals(resource);
            case "lock" -> "timesheet".equals(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForDirector(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "employee").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            case "export" -> "report".equals(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForRegionDirector(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "employee").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            case "export" -> "report".equals(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForDeptHead(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "employee", "notification").contains(resource);
            case "create" -> Set.of("attendance", "leave").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            case "export" -> "report".equals(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForDeputyHead(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "notification").contains(resource);
            case "create" -> Set.of("attendance", "leave").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            case "export" -> "report".equals(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForUnitHead(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "report", "notification").contains(resource);
            case "create" -> Set.of("attendance", "leave").contains(resource);
            case "approve" -> Set.of("timesheet", "leave").contains(resource);
            default -> false;
        };
    }

    private static boolean isAllowedForEmployee(String action, String resource) {
        return switch (action) {
            case "view" -> Set.of("attendance", "timesheet", "leave", "notification", "report").contains(resource);
            case "create" -> Set.of("attendance", "leave").contains(resource);
            case "delete" -> "leave".equals(resource);
            default -> false;
        };
    }

    private static String resolveScopeStatic(String role) {
        return switch (role) {
            case "SYSTEM_ADMIN", "CEO", "DIVISION_DIRECTOR" -> "all";
            case "REGION_DIRECTOR" -> "region";
            case "DEPT_HEAD", "DEPUTY_HEAD" -> "department";
            case "UNIT_HEAD" -> "unit";
            default -> "self";
        };
    }
}
