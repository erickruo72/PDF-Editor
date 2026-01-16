const fileInput = document.getElementById("fileInput");
const pdfContainer = document.getElementById("pdf-container");
const saveBtn = document.getElementById("saveBtn");

let currentFile;
let edits = [];
let pageViewports = [];

const SCALE = 1.5;

fileInput.addEventListener("change", async (e) => {
    currentFile = e.target.files[0];
    if (!currentFile) return;

    pdfContainer.innerHTML = "";
    edits = [];
    pageViewports = [];

    await renderPdf(currentFile);
    await loadWordBoxes(currentFile);
});

async function renderPdf(file) {
    const buffer = await file.arrayBuffer();
    const pdf = await pdfjsLib.getDocument(buffer).promise;

    for (let i = 1; i <= pdf.numPages; i++) {
        const page = await pdf.getPage(i);
        const viewport = page.getViewport({ scale: SCALE });
        pageViewports.push(viewport);

        const pageDiv = document.createElement("div");
        pageDiv.className = "page";
        pageDiv.style.width = viewport.width + "px";
        pageDiv.style.height = viewport.height + "px";

        const canvas = document.createElement("canvas");
        canvas.width = viewport.width;
        canvas.height = viewport.height;

        const ctx = canvas.getContext("2d");
        await page.render({ canvasContext: ctx, viewport }).promise;

        pageDiv.appendChild(canvas);
        pdfContainer.appendChild(pageDiv);
    }
}

async function loadWordBoxes(file) {
    const formData = new FormData();
    formData.append("file", file);

    const res = await fetch("/api/pdf/words", {
        method: "POST",
        body: formData
    });

    const words = await res.json();
    const pages = document.querySelectorAll(".page");

    words.forEach(word => {
        const pageDiv = pages[word.page];
        const viewport = pageViewports[word.page];
        if (!pageDiv || !viewport) return;

        // ---- COORDINATE CONVERSION (CRITICAL FIX) ----
        const BASELINE_ADJUST = word.height * 2.8;

        const x = word.x * SCALE;
        const y =
            viewport.height -
            (word.y * SCALE) -
            BASELINE_ADJUST;


        const w = word.width * SCALE;
        const h = word.height * SCALE;

        const box = document.createElement("div");
        box.className = "word";
        box.style.left = x + "px";
        box.style.top = y + "px";
        box.style.width = w + "px";
        box.style.height = h + "px";

        // Editable overlay (NO original text duplication)
        box.contentEditable = "true";
        box.innerText = word.text;
        box.dataset.original = word.text;

        box.addEventListener("focus", () => {
            box.classList.add("editing");
        });

        box.addEventListener("blur", () => {
            box.classList.remove("editing");

            const newText = box.innerText.trim();
            if (newText !== box.dataset.original) {
                edits = edits.filter(e =>
                    !(e.page === word.page &&
                        e.x === word.x &&
                        e.y === word.y)
                );

                edits.push({
                    page: word.page,
                    x: word.x,
                    y: word.y,
                    width: word.width,
                    height: word.height,
                    oldText: word.text,
                    newText
                });
            }
        });

        pageDiv.appendChild(box);
    });
}

saveBtn.addEventListener("click", async () => {
    if (!currentFile || edits.length === 0) {
        alert("No edits made");
        return;
    }

    const formData = new FormData();
    formData.append("file", currentFile);
    formData.append("edits", JSON.stringify(edits));

    const res = await fetch("/api/pdf/edit", {
        method: "POST",
        body: formData
    });

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = "edited.pdf";
    a.click();
});
