export async function copyText(value) {
    if (!value) return;

    await navigator.clipboard.writeText(value);
}