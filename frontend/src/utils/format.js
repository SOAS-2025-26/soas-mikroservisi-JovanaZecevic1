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
