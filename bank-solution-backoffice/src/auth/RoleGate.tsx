import React from 'react';
import {BackofficeRole} from '@/types';
import {useAuth} from './AuthContext';

interface RoleGateProps {
    role: BackofficeRole;
    fallback?: React.ReactNode;
    children: React.ReactNode;
}

export const RoleGate: React.FC<RoleGateProps> = ({role, fallback = null, children}) => {
    const {hasRole} = useAuth();
    return <>{hasRole(role) ? children : fallback}</>;
};

export const AccessDenied: React.FC = () => (
    <div className="flex flex-col items-center gap-2 py-16 text-center">
        <h1 className="text-xl font-semibold text-gray-900">Access denied</h1>
        <p className="text-gray-600">Your role does not allow this action.</p>
    </div>
);
