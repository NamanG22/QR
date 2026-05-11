import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";

const container = document.getElementById("tw-root");
if (container) {
    createRoot(container).render(
        <StrictMode>
            <App />
        </StrictMode>,
    );
}
