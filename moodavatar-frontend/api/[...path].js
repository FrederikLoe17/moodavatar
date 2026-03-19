export default async function handler(req, res) {
    const backendUrl = process.env.BACKEND_URL
    if (!backendUrl) {
        res.status(500).json({ error: 'BACKEND_URL not configured' })
        return
    }

    const path = req.url.replace(/^\/api/, '') || '/'
    const targetUrl = `${backendUrl}${path}`

    const forwardHeaders = {}
    for (const [key, value] of Object.entries(req.headers)) {
        if (!['host', 'connection', 'transfer-encoding', 'content-length'].includes(key.toLowerCase())) {
            forwardHeaders[key] = value
        }
    }

    let body
    if (req.method !== 'GET' && req.method !== 'HEAD' && req.body !== undefined) {
        body = JSON.stringify(req.body)
        forwardHeaders['content-type'] = 'application/json'
        forwardHeaders['content-length'] = Buffer.byteLength(body).toString()
    }

    const response = await fetch(targetUrl, { method: req.method, headers: forwardHeaders, body })

    for (const [key, value] of response.headers.entries()) {
        if (!['transfer-encoding', 'connection'].includes(key.toLowerCase())) {
            res.setHeader(key, value)
        }
    }

    res.status(response.status).end(Buffer.from(await response.arrayBuffer()))
}
