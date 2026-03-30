import type { PropsWithChildren, ReactNode } from 'react';

export function PageSection({ title, actions, children }: PropsWithChildren<{ title: string; actions?: ReactNode }>) {
  return (
    <section className="page-section">
      <div className="page-section-header">
        <h2>{title}</h2>
        {actions}
      </div>
      {children}
    </section>
  );
}
