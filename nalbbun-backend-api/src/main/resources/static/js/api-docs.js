async function loadCatalog() {
  const container = document.getElementById('catalogContainer');
  container.innerHTML = '<p>Loading...</p>';
  try {
    const response = await fetch('/api/catalog');
    const data = await response.json();
    container.innerHTML = '';
    for (const group of data.groups) {
      const card = document.createElement('section');
      card.className = 'catalog-card';
      card.innerHTML = `
        <h3>${group.name}</h3>
        <div class="endpoint-list">
          ${group.endpoints.map(endpoint => `
            <article class="endpoint-item">
              <div class="endpoint-top">
                <span class="badge">${endpoint.method}</span>
                <span class="path">${endpoint.path}</span>
              </div>
              <div><strong>${endpoint.title}</strong></div>
              <div class="notes">${endpoint.notes}</div>
            </article>
          `).join('')}
        </div>
      `;
      container.appendChild(card);
    }
  } catch (error) {
    container.innerHTML = `<p>API catalog 로딩 실패: ${error}</p>`;
  }
}
document.getElementById('reloadButton').addEventListener('click', loadCatalog);
loadCatalog();
