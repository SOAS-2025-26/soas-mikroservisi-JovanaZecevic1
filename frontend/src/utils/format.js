// Always shows exactly 2 decimals with thousands separators. The one exception:
// if rounding to 2 decimals would collapse the value to "0.00" (abs < 0.01),
// decimals are extended (up to 6) until the first significant digit is visible -
// otherwise small crypto ratios would silently lose all their information.
export function formatNumber(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '';
  if (num === 0) return '0.00';

  const abs = Math.abs(num);
  let decimals = 2;
  if (abs < 0.01) {
    while (decimals < 6 && parseFloat(abs.toFixed(decimals)) === 0) {
      decimals++;
    }
  }

  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

// Same rounding rule as formatNumber (2 decimals, extended up to 6 for sub-0.01
// values), but returns a plain dot-decimal string with no thousands separator -
// safe to drop straight into a number input's value.
export function roundForInput(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '';
  if (num === 0) return '0.00';

  const abs = Math.abs(num);
  let decimals = 2;
  if (abs < 0.01) {
    while (decimals < 6 && parseFloat(abs.toFixed(decimals)) === 0) {
      decimals++;
    }
  }

  return num.toFixed(decimals);
}
