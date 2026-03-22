export function showToast(message) {
    const toast = document.createElement("div");
    toast.className = "app-toast";
    toast.textContent = message;

    document.body.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.add("app-toast--visible");
    });

    setTimeout(() => {
        toast.classList.remove("app-toast--visible");
        setTimeout(() => {
            toast.remove();
        }, 200);
    }, 1500);
}