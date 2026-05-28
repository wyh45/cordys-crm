export interface SseOptions {
  url: string;
  body: Record<string, unknown>;
  signal?: AbortSignal;
  onData: (event: string, payload: string) => void;
  onDone?: (fullText: string) => void;
  onError?: (err: string) => void;
}

export async function useSseStream(options: SseOptions): Promise<void> {
  const { url, body, signal, onData, onDone, onError } = options;
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal,
  });
  const reader = resp.body?.getReader();
  if (!reader) throw new Error('No response stream');

  const decoder = new TextDecoder();
  let buffer = '';
  let fullText = '';

  function processLines() {
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';
    let eventName = '';
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (line.startsWith('event:')) {
        eventName = line.substring(6).trim();
      } else if (line.startsWith('data:') && eventName) {
        const payload = line.substring(5).trim();
        if (eventName === 'done') {
          onDone?.(fullText);
          return;
        }
        if (eventName.endsWith('_end')) {
          fullText = payload;
          onDone?.(fullText);
          return;
        }
        if (eventName === 'error') {
          onError?.(payload);
        } else {
          fullText += payload;
          onData(eventName, payload);
        }
        eventName = '';
      }
    }
  }

  try {
    while (true) {
      // eslint-disable-next-line no-await-in-loop
      const { done, value } = await reader.read();
      if (done) {
        buffer += decoder.decode();
        processLines();
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      processLines();
    }
  } catch (error: unknown) {
    if (error instanceof DOMException && error.name === 'AbortError') return;
    throw error;
  }
}
