// Fiat currencies offered in the UI. floatrates.com supports many more; this is a
// curated subset covering the major ones plus RSD (used throughout the project).
export const FIAT_CURRENCIES = ['EUR', 'USD', 'RSD', 'GBP', 'CHF', 'JPY', 'AUD', 'CAD', 'CNY', 'RUB'];

// Must match the symbols supported by crypto-exchange-service's SYMBOL_TO_COINGECKO_ID map.
export const CRYPTO_CURRENCIES = [
  'BTC', 'ETH', 'USDT', 'BNB', 'SOL', 'XRP', 'USDC', 'ADA', 'DOGE', 'TRX',
  'TON', 'DOT', 'MATIC', 'LTC', 'AVAX', 'SHIB', 'LINK', 'ATOM', 'XLM', 'BCH',
  'UNI', 'ETC', 'NEAR', 'ALGO', 'VET', 'ICP', 'FIL', 'APT', 'ARB', 'OP',
];

export const TRADE_CURRENCIES = [...CRYPTO_CURRENCIES, ...FIAT_CURRENCIES];
