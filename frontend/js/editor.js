/**
 * editor.js — Monaco Editor wrapper for the practice view.
 * Loads Monaco via CDN (AMD loader) and exposes init/getValue helpers.
 */

const codeEditor = {
    instance: null,
    ready: false,

    /**
     * Initialize Monaco editor inside the given container element.
     * @param {string} containerId - DOM id of the editor container
     * @returns {Promise<void>}
     */
    init(containerId) {
        return new Promise((resolve, reject) => {
            if (this.instance) {
                resolve();
                return;
            }

            // Monaco AMD loader is loaded in index.html
            require.config({
                paths: {
                    vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs',
                },
            });

            require(['vs/editor/editor.main'], () => {
                // Define a custom dark theme matching Code Duel's palette
                monaco.editor.defineTheme('codeduel-dark', {
                    base: 'vs-dark',
                    inherit: true,
                    rules: [
                        { token: 'comment', foreground: '64748b', fontStyle: 'italic' },
                        { token: 'keyword', foreground: '818cf8' },
                        { token: 'string', foreground: '34d399' },
                        { token: 'number', foreground: 'fbbf24' },
                        { token: 'type', foreground: '38bdf8' },
                    ],
                    colors: {
                        'editor.background': '#0f172a',
                        'editor.foreground': '#f1f5f9',
                        'editor.lineHighlightBackground': '#1e293b',
                        'editorCursor.foreground': '#38bdf8',
                        'editor.selectionBackground': '#334155',
                        'editorLineNumber.foreground': '#475569',
                        'editorLineNumber.activeForeground': '#94a3b8',
                    },
                });

                this.instance = monaco.editor.create(
                    document.getElementById(containerId),
                    {
                        value: '# Write your Python solution here\n\n',
                        language: 'python',
                        theme: 'codeduel-dark',
                        fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                        fontSize: 14,
                        lineHeight: 22,
                        minimap: { enabled: false },
                        scrollBeyondLastLine: false,
                        automaticLayout: true,
                        padding: { top: 16, bottom: 16 },
                        renderLineHighlight: 'all',
                        tabSize: 4,
                        wordWrap: 'on',
                    }
                );

                this.ready = true;
                resolve();
            });
        });
    },

    /**
     * Get the current code from the editor.
     */
    getValue() {
        return this.instance ? this.instance.getValue() : '';
    },

    /**
     * Set code in the editor.
     */
    setValue(code) {
        if (this.instance) {
            this.instance.setValue(code);
        }
    },

    /**
     * Destroy the editor instance (for cleanup).
     */
    dispose() {
        if (this.instance) {
            this.instance.dispose();
            this.instance = null;
            this.ready = false;
        }
    },
};
