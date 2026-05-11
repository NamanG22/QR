const buttons = document.querySelectorAll(".toolbar button[data-board]");
const utsBoard = document.getElementById("utsBoard");
const prsBoard = document.getElementById("prsBoard");

function show(which) {
    if (which === "prs") {
        utsBoard.classList.remove("active");
        prsBoard.classList.add("active");
    } else {
        prsBoard.classList.remove("active");
        utsBoard.classList.add("active");
    }
    buttons.forEach((b) => {
        b.classList.toggle("active", b.dataset.board === which);
    });
}

buttons.forEach((b) => {
    b.addEventListener("click", () => show(b.dataset.board));
});

const footer = document.getElementById("footerLastUpdated");
if (footer) {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    footer.textContent =
        `Last updated: ${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
        `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}
