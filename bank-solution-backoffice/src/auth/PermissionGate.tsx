import React from 'react';
import {Permission} from '@/types';
import {useAuth} from './AuthContext';

interface PermissionGateProps {
    permission: Permission | Permission[];
    fallback?: React.ReactNode;
    children: React.ReactNode;
}

export const PermissionGate: React.FC<PermissionGateProps> = ({permission, fallback = null, children}) => {
    const {hasPermission} = useAuth();
    const requiredPermissions = Array.isArray(permission) ? permission : [permission];
    return <>{requiredPermissions.some(hasPermission) ? children : fallback}</>;
};

export const AccessDenied: React.FC = () => (
    <div className="flex flex-col items-center gap-2 py-16 text-center">
        <h1 className="text-xl font-semibold text-gray-900">Access denied</h1>
        <p className="text-gray-600">Your role does not allow this action.</p>
    </div>
);
