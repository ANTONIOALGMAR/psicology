/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f2fbf9',
          100: '#dff7f1',
          200: '#bfeee3',
          300: '#8fded0',
          400: '#56c6b7',
          500: '#34a99d',
          600: '#288880',
          700: '#246d68',
          800: '#215754',
          900: '#1f4846'
        }
      },
      boxShadow: {
        soft: '0 10px 30px rgba(31, 72, 70, 0.12)'
      }
    },
  },
  plugins: [],
}
