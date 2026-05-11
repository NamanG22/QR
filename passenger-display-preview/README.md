# Passenger Display Preview

Standalone Vite + Tailwind CSS project that renders the QFRDS RDSO passenger display
(UTS / PRS) at its native 1024 × 768 canvas. Source-of-truth for the
visual design — keep this in sync with
`qfrds-controller-simulator/src/main/resources/fxml/passenger_display.fxml`
and `…/styles/passenger_display.css`.

## Run

```bash
cd passenger-display-preview
npm install
npm run dev
```

Vite will print a local URL (typically <http://localhost:5173>). Open it
in a browser. Use the **UTS layout / PRS layout** buttons in the toolbar
to switch boards.

## Build

```bash
npm run build      # writes static site to dist/
npm run preview    # serves the built site
```

## Files

- `index.html` — the markup that mirrors the FXML structure.
- `src/styles.css` — the styling that mirrors `passenger_display.css`.
- `src/main.js` — UTS/PRS toggle + footer timestamp.
