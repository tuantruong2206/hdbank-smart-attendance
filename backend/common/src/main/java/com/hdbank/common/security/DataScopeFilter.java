package com.hdbank.common.security;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Data scope filtering based on user role and organization hierarchy.
 * Returns predicates for JPA query filtering to enforce row-level security.
 *
 * Scope levels:
 * - SELF: employee sees only own data
 * - UNIT: unit head sees their bo phan
 * - DEPARTMENT: dept head sees their phong
 * - BRANCH: branch manager sees their chi nhanh
 * - REGION: region director sees their vung
 * - ALL: system admin, CEO sees everything
 */
public class DataScopeFilter {

    public enum DataScope {
        SELF, UNIT, DEPARTMENT, BRANCH, REGION, ALL
    }

    /**
     * Resolves the data scope for a given role.
     */
    public static DataScope resolveScope(String role) {
        if (role == null) {
            return DataScope.SELF;
        }
        return switch (role) {
            case "SYSTEM_ADMIN", "CEO" -> DataScope.ALL;
            case "REGION_DIRECTOR" -> DataScope.REGION;
            case "DIVISION_DIRECTOR" -> DataScope.BRANCH;
            case "DEPT_HEAD", "DEPUTY_HEAD" -> DataScope.DEPARTMENT;
            case "UNIT_HEAD" -> DataScope.UNIT;
            default -> DataScope.SELF;
        };
    }

    /**
     * Creates a predicate that checks whether a given employeeId/organizationId pair
     * falls within the user's data scope.
     *
     * @param principal the current user's principal
     * @return a DataScopeContext containing scope info for query building
     */
    public static DataScopeContext buildContext(UserPrincipal principal) {
        DataScope scope = resolveScope(principal.getRole());
        return new DataScopeContext(scope, principal.getId(), principal.getOrganizationId());
    }

    /**
     * Context object that holds the resolved data scope and user information.
     * Services use this to append WHERE clauses to JPA queries.
     */
    public record DataScopeContext(
            DataScope scope,
            UUID userId,
            UUID organizationId
    ) {
        /**
         * Returns true if the scope allows unrestricted access (ALL).
         */
        public boolean isUnrestricted() {
            return scope == DataScope.ALL;
        }

        /**
         * Returns true if the scope is limited to the user's own data only.
         */
        public boolean isSelfOnly() {
            return scope == DataScope.SELF;
        }

        /**
         * Returns a JPQL WHERE clause fragment for filtering by data scope.
         * The caller must bind :scopeUserId and :scopeOrgId parameters.
         *
         * Example usage:
         *   String whereClause = context.toJpqlWhereClause("e.employeeId", "e.organizationId");
         *   query.append(" AND ").append(whereClause);
         *
         * @param employeeIdField JPQL field for employee ID
         * @param orgIdField JPQL field for organization ID
         * @return JPQL WHERE clause fragment
         */
        public String toJpqlWhereClause(String employeeIdField, String orgIdField) {
            return switch (scope) {
                case ALL -> "1=1";
                case SELF -> employeeIdField + " = :scopeUserId";
                case UNIT, DEPARTMENT, BRANCH, REGION ->
                        orgIdField + " = :scopeOrgId";
            };
        }
    }
}
