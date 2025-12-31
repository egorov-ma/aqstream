import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { formatIcsDate, escapeIcs, generateIcsContent, downloadFile } from './calendar';

describe('formatIcsDate', () => {
  it('formats ISO date to ICS format', () => {
    // 2024-03-15T14:30:00.000Z
    const result = formatIcsDate('2024-03-15T14:30:00.000Z');
    expect(result).toBe('20240315T143000Z');
  });

  it('handles midnight correctly', () => {
    const result = formatIcsDate('2024-01-01T00:00:00.000Z');
    expect(result).toBe('20240101T000000Z');
  });

  it('handles end of day correctly', () => {
    const result = formatIcsDate('2024-12-31T23:59:59.000Z');
    expect(result).toBe('20241231T235959Z');
  });

  it('pads single digit values', () => {
    const result = formatIcsDate('2024-01-05T09:05:05.000Z');
    expect(result).toBe('20240105T090505Z');
  });
});

describe('escapeIcs', () => {
  it('returns empty string for empty input', () => {
    expect(escapeIcs('')).toBe('');
  });

  it('returns same string if no special characters', () => {
    expect(escapeIcs('Hello World')).toBe('Hello World');
  });

  it('escapes backslash', () => {
    expect(escapeIcs('path\\to\\file')).toBe('path\\\\to\\\\file');
  });

  it('escapes semicolon', () => {
    expect(escapeIcs('item1;item2')).toBe('item1\\;item2');
  });

  it('escapes comma', () => {
    expect(escapeIcs('item1,item2')).toBe('item1\\,item2');
  });

  it('replaces newlines with \\n', () => {
    expect(escapeIcs('line1\nline2')).toBe('line1\\nline2');
  });

  it('handles Windows line endings', () => {
    expect(escapeIcs('line1\r\nline2')).toBe('line1\\nline2');
  });

  it('escapes multiple special characters', () => {
    expect(escapeIcs('a\\b;c,d\ne')).toBe('a\\\\b\\;c\\,d\\ne');
  });

  it('handles Russian text', () => {
    expect(escapeIcs('Привет, мир!')).toBe('Привет\\, мир!');
  });
});

describe('generateIcsContent', () => {
  const baseEvent = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    title: 'Test Event',
    startsAt: '2024-03-15T14:00:00.000Z',
  };

  it('generates valid ICS content with required fields', () => {
    const content = generateIcsContent(baseEvent);

    expect(content).toContain('BEGIN:VCALENDAR');
    expect(content).toContain('VERSION:2.0');
    expect(content).toContain('PRODID:-//AqStream//Event//RU');
    expect(content).toContain('BEGIN:VEVENT');
    expect(content).toContain(`UID:${baseEvent.id}@aqstream.ru`);
    expect(content).toContain('DTSTART:20240315T140000Z');
    expect(content).toContain('SUMMARY:Test Event');
    expect(content).toContain('END:VEVENT');
    expect(content).toContain('END:VCALENDAR');
  });

  it('includes end date when provided', () => {
    const content = generateIcsContent({
      ...baseEvent,
      endsAt: '2024-03-15T16:00:00.000Z',
    });

    expect(content).toContain('DTEND:20240315T160000Z');
  });

  it('does not include DTEND when endsAt is null', () => {
    const content = generateIcsContent({
      ...baseEvent,
      endsAt: null,
    });

    expect(content).not.toContain('DTEND:');
  });

  it('includes description when provided', () => {
    const content = generateIcsContent({
      ...baseEvent,
      description: 'This is a test event',
    });

    expect(content).toContain('DESCRIPTION:This is a test event');
  });

  it('truncates long description to 500 chars', () => {
    const longDescription = 'a'.repeat(600);
    const content = generateIcsContent({
      ...baseEvent,
      description: longDescription,
    });

    // Проверяем, что описание обрезано до 500 символов
    expect(content).toContain('DESCRIPTION:' + 'a'.repeat(500));
    expect(content).not.toContain('a'.repeat(501));
  });

  it('includes location when provided', () => {
    const content = generateIcsContent({
      ...baseEvent,
      location: 'Москва, ул. Пушкина, д. 1',
    });

    expect(content).toContain('LOCATION:Москва\\, ул. Пушкина\\, д. 1');
  });

  it('escapes special characters in title', () => {
    const content = generateIcsContent({
      ...baseEvent,
      title: 'Event; with, special\\chars',
    });

    expect(content).toContain('SUMMARY:Event\\; with\\, special\\\\chars');
  });

  it('uses CRLF line endings', () => {
    const content = generateIcsContent(baseEvent);

    // ICS требует \r\n как разделитель строк
    expect(content).toContain('\r\n');
    expect(content.split('\r\n').length).toBeGreaterThan(1);
  });

  it('includes DTSTAMP', () => {
    const content = generateIcsContent(baseEvent);

    // DTSTAMP должен быть в формате ICS
    expect(content).toMatch(/DTSTAMP:\d{8}T\d{6}Z/);
  });
});

describe('downloadFile', () => {
  let mockLink: HTMLAnchorElement;
  let createObjectURLMock: ReturnType<typeof vi.fn>;
  let revokeObjectURLMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    // Мокаем createElement
    mockLink = {
      href: '',
      download: '',
      style: { display: '' },
      click: vi.fn(),
    } as unknown as HTMLAnchorElement;

    vi.spyOn(document, 'createElement').mockReturnValue(mockLink);
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink);
    vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink);

    // Мокаем URL API
    createObjectURLMock = vi.fn().mockReturnValue('blob:http://localhost/mock-blob-url');
    revokeObjectURLMock = vi.fn();

    vi.stubGlobal('URL', {
      createObjectURL: createObjectURLMock,
      revokeObjectURL: revokeObjectURLMock,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('creates a Blob with correct content and type', () => {
    const content = 'test content';
    const mimeType = 'text/plain';

    downloadFile(content, 'test.txt', mimeType);

    expect(createObjectURLMock).toHaveBeenCalledTimes(1);
    const blob = createObjectURLMock.mock.calls[0][0] as Blob;
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.type).toBe(mimeType);
  });

  it('sets correct href and download attributes', () => {
    downloadFile('content', 'myfile.ics', 'text/calendar');

    expect(mockLink.href).toBe('blob:http://localhost/mock-blob-url');
    expect(mockLink.download).toBe('myfile.ics');
  });

  it('triggers click on the link', () => {
    downloadFile('content', 'file.txt', 'text/plain');

    expect(mockLink.click).toHaveBeenCalledTimes(1);
  });

  it('appends and removes the link from DOM', () => {
    downloadFile('content', 'file.txt', 'text/plain');

    expect(document.body.appendChild).toHaveBeenCalledWith(mockLink);
    expect(document.body.removeChild).toHaveBeenCalledWith(mockLink);
  });

  it('revokes the object URL after download', () => {
    downloadFile('content', 'file.txt', 'text/plain');

    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:http://localhost/mock-blob-url');
  });

  it('hides the link element', () => {
    downloadFile('content', 'file.txt', 'text/plain');

    expect(mockLink.style.display).toBe('none');
  });

  it('works with ICS content', () => {
    const icsContent = generateIcsContent({
      id: 'test-id',
      title: 'Test Event',
      startsAt: '2024-03-15T14:00:00.000Z',
    });

    downloadFile(icsContent, 'event.ics', 'text/calendar');

    expect(mockLink.download).toBe('event.ics');
    expect(mockLink.click).toHaveBeenCalled();
  });
});
