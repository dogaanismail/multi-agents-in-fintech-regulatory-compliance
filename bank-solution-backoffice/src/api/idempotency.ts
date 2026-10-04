export const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';

export const generateIdempotencyKey = (): string =>
    Array.from(crypto.getRandomValues(new Uint8Array(16)), (randomByte) =>
        randomByte.toString(16).padStart(2, '0')
    ).join('');
