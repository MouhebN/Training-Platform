const DATE_TIME_FORMAT = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
});

export function formatDateTime24(value?: string): string {
  if (!value) return 'Not set';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : DATE_TIME_FORMAT.format(date);
}

export function toApiDateTime(value: string): string {
  return value.trim().replace(' ', 'T');
}

export function toDateTimeInput24(value: string): string {
  return value.slice(0, 16).replace(' ', 'T');
}

export function formatTime24(value?: string): string {
  return value ? value.slice(0, 5) : '--:--';
}
