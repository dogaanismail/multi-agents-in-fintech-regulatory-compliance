export const KEYCLOAK_LOGIN_PATH = '/oauth2/authorization/keycloak';
export const LOGOUT_PATH = '/logout';
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';

export const redirectToLogin = (): void => {
    window.location.assign(KEYCLOAK_LOGIN_PATH);
};

export const readCsrfToken = (): string => {
    const csrfCookie = document.cookie
        .split('; ')
        .find((cookie) => cookie.startsWith(`${CSRF_COOKIE_NAME}=`));
    return csrfCookie ? decodeURIComponent(csrfCookie.substring(CSRF_COOKIE_NAME.length + 1)) : '';
};
