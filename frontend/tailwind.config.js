/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'p1': '#dc2626',
        'p2': '#ea580c',
        'p3': '#ca8a04',
        'p4': '#16a34a',
        'status-open': '#3b82f6',
        'status-investigating': '#8b5cf6',
        'status-mitigated': '#f59e0b',
        'status-closed': '#6b7280',
      }
    },
  },
  plugins: [],
}
