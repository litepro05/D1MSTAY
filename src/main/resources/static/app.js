// Konfirmasi dipasang pada formulir agar tindakan tetap memakai POST dan token CSRF.
document.addEventListener('submit', (event) => {
  const message = event.target.dataset.confirm;
  if (message && !window.confirm(message)) {
    event.preventDefault();
    return;
  }
  if (event.defaultPrevented || event.target.method.toLowerCase() !== 'post') return;
  const button = event.submitter;
  if (button) {
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
  }
});

document.querySelectorAll('[data-dismiss-notice]').forEach((button) => {
  button.addEventListener('click', () => button.closest('.alert').remove());
});

// Pulihkan tombol ketika pengguna kembali lewat riwayat browser.
window.addEventListener('pageshow', () => {
  document.querySelectorAll('button[aria-busy="true"]').forEach((button) => {
    button.disabled = false;
    button.removeAttribute('aria-busy');
  });
});

document.querySelectorAll('[data-print]').forEach((button) => {
  button.addEventListener('click', () => window.print());
});
