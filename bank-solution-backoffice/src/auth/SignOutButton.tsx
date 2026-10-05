import React from 'react';
import {LOGOUT_PATH, readCsrfToken} from './session';

export const SignOutButton: React.FC = () => {
    const attachCsrfToken = (event: React.FormEvent<HTMLFormElement>) => {
        const csrfInput = event.currentTarget.elements.namedItem('_csrf') as HTMLInputElement;
        csrfInput.value = readCsrfToken();
    };

    return (
        <form method="post" action={LOGOUT_PATH} onSubmit={attachCsrfToken}>
            <input type="hidden" name="_csrf"/>
            <button
                type="submit"
                className="text-sm font-medium text-gray-600 hover:text-gray-900 border border-gray-300 rounded-md px-3 py-1"
            >
                Sign out
            </button>
        </form>
    );
};
