// patch옵션 통일용
export async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
        },
        ...options,
    });

    const contentType = response.headers.get("content-type") || "";
    let body = null;

    if (contentType.includes("application/json")) {
        body = await response.json().catch(() => null);
    } else {
        const text = await response.text().catch(() => "");
        body = text ? { message: text } : null;
    }

    if (!response.ok) {
        const error = new Error("HTTP request failed");
        error.status = response.status;
        error.body = body;
        throw error;
    }

    return body;
}