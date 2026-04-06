export type GlobalNoticeTone = 'success' | 'error' | 'info';

const EVENT_NAME = 'nalbbun:ui-feedback';

type FeedbackEventDetail =
  | { type: 'request-start'; message?: string }
  | { type: 'request-end' }
  | { type: 'notify'; tone: GlobalNoticeTone; message: string };

function emit(detail: FeedbackEventDetail) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent<FeedbackEventDetail>(EVENT_NAME, { detail }));
}

export function beginGlobalLoading(message?: string) {
  emit({ type: 'request-start', message });
}

export function endGlobalLoading() {
  emit({ type: 'request-end' });
}

export function notifyGlobal(message: string, tone: GlobalNoticeTone = 'info') {
  emit({ type: 'notify', message, tone });
}

export function subscribeUiFeedback(listener: (detail: FeedbackEventDetail) => void) {
  if (typeof window === 'undefined') return () => undefined;
  const handler = (event: Event) => {
    const customEvent = event as CustomEvent<FeedbackEventDetail>;
    if (customEvent.detail) listener(customEvent.detail);
  };
  window.addEventListener(EVENT_NAME, handler as EventListener);
  return () => window.removeEventListener(EVENT_NAME, handler as EventListener);
}


export function notifyUi(payload: { type?: 'notify'; tone?: GlobalNoticeTone; message: string }) {
  notifyGlobal(payload.message, payload.tone ?? 'info');
}
