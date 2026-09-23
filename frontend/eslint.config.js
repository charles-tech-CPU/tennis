// Configuration ESLint (format "flat config", ESLint 9+).
// "npm run lint" verifie, "npm run lint:fix" corrige ce qui peut l'etre automatiquement.
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  { ignores: ['dist/**', 'node_modules/**'] },

  // Regles JavaScript recommandees (variables inutilisees, code inaccessible...)
  js.configs.recommended,

  // Regles Vue 3 recommandees (v-for sans :key, props mal declarees...)
  ...pluginVue.configs['flat/recommended'],

  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.browser
    },
    // ESLint sert ici a detecter des erreurs, pas a imposer une mise en page :
    // on coupe les regles purement cosmetiques du template (un attribut par
    // ligne, indentation, <img /> vs <img>...).
    rules: {
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/html-indent': 'off',
      'vue/html-self-closing': 'off'
    }
  }
]
