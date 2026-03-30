import type { PropsWithChildren, ReactNode } from 'react';

export function AppCard({ title, description, actions, children }: PropsWithChildren<{ title?: string; description?: string; actions?: ReactNode }>) {
  return (
    <section className="app-card">
      {(title || actions) && (
        <div className="app-card-header">
          <div>
            {title && <h2>{title}</h2>}
            {description && <p className="muted">{description}</p>}
          </div>
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className="app-card-body">{children}</div>
    </section>
  );
}
