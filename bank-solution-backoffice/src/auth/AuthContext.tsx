import React, {createContext, useContext, useEffect, useState} from 'react';
import {authService} from '@/api';
import {CurrentUser, Permission} from '@/types';
import {LoadingSpinner} from '@/components/common';
import {SignOutButton} from './SignOutButton';

interface AuthContextValue {
    currentUser: CurrentUser;
  hasPermission: (permission: Permission) => boolean;
}

type SessionProblem = 'no-backoffice-role' | 'gateway-unavailable';

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({children}) => {
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [sessionProblem, setSessionProblem] = useState<SessionProblem | null>(null);

    useEffect(() => {
        authService
            .getCurrentUser()
            .then(setCurrentUser)
            .catch((error) => {
                const status = error.response?.status;
                if (status === 403) {
                    setSessionProblem('no-backoffice-role');
                } else if (status !== 401) {
                    setSessionProblem('gateway-unavailable');
                }
            });
    }, []);

    if (sessionProblem) {
        return <SessionProblemScreen sessionProblem={sessionProblem}/>;
    }

    if (!currentUser) {
        return (
            <div className="flex justify-center items-center h-screen">
                <LoadingSpinner size="lg"/>
            </div>
        );
    }

  const hasPermission = (permission: Permission): boolean => currentUser.permissions.includes(permission);

  return <AuthContext.Provider value={{currentUser, hasPermission}}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
    const auth = useContext(AuthContext);
    if (!auth) {
        throw new Error('useAuth must be used inside AuthProvider');
    }
    return auth;
};

const SessionProblemScreen: React.FC<{ sessionProblem: SessionProblem }> = ({sessionProblem}) => (
    <div className="flex flex-col justify-center items-center h-screen gap-4 text-center px-4">
        {sessionProblem === 'no-backoffice-role' ? (
            <>
                <h1 className="text-xl font-semibold text-gray-900">No backoffice access</h1>
                <p className="text-gray-600">Your account is signed in but has no backoffice role. Ask an administrator
                    for one.</p>
                <SignOutButton/>
            </>
        ) : (
            <>
                <h1 className="text-xl font-semibold text-gray-900">Backoffice unavailable</h1>
                <p className="text-gray-600">The gateway did not respond. Try again in a moment.</p>
            </>
        )}
    </div>
);
