import React, { useEffect } from 'react';
import { usePermission } from '@/shared/hooks/usePermission';
import { useAuth } from '@/shared/hooks/useAuth';

interface PermissionGateProps {
  /** The action to check (e.g., "view", "create", "approve", "delete", "export", "lock", "edit") */
  action: string;
  /** The resource to check (e.g., "attendance", "timesheet", "leave", "admin", "report", "employee", "organization", "notification") */
  resource: string;
  /** Content to render when the user has the required permission */
  children: React.ReactNode;
  /** Optional fallback content when the user lacks the required permission */
  fallback?: React.ReactNode;
}

/**
 * PermissionGate conditionally renders children based on the current user's
 * RBAC permissions. It fetches permissions on mount if they have not been
 * loaded yet, and hides the wrapped content if the user lacks the required
 * action + resource pair.
 *
 * Usage:
 *   <PermissionGate action="approve" resource="leave">
 *     <ApproveButton />
 *   </PermissionGate>
 */
const PermissionGate: React.FC<PermissionGateProps> = ({
  action,
  resource,
  children,
  fallback = null,
}) => {
  const { isAuthenticated } = useAuth();
  const { canAccess, isLoaded, fetchPermissions } = usePermission();

  useEffect(() => {
    if (isAuthenticated && !isLoaded) {
      fetchPermissions();
    }
  }, [isAuthenticated, isLoaded, fetchPermissions]);

  if (!isAuthenticated) {
    return <>{fallback}</>;
  }

  if (!isLoaded) {
    // Permissions still loading -- render nothing rather than flash forbidden content
    return null;
  }

  if (canAccess(action, resource)) {
    return <>{children}</>;
  }

  return <>{fallback}</>;
};

export default PermissionGate;
