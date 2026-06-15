package project_biu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a simplified HTTP/1.1 request out of a {@link BufferedReader} into a
 * {@link RequestInfo} value object.
 *
 * <p>The expected wire format is:
 * <pre>
 *   &lt;METHOD&gt; &lt;URI&gt; HTTP/1.1
 *   &lt;header lines&gt;
 *   &lt;blank line&gt;
 *   &lt;optional form-style parameter lines, e.g. filename="x.txt"&gt;
 *   &lt;blank line&gt;
 *   &lt;content lines&gt;
 *   &lt;blank line or EOF&gt;
 * </pre>
 *
 * <p>Query parameters in the URI and the form-style parameter lines are
 * collected into the same {@code parameters} map.
 */
public class RequestParser {

    /** Utility class; not meant to be instantiated. */
    public RequestParser() { /* no setup needed */ }

    /**
     * Parses a single HTTP request off the given reader.
     *
     * @param reader buffered reader positioned at the start of a request
     * @return a populated {@link RequestInfo} (never {@code null})
     * @throws IOException if reading from the underlying stream fails
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {
        // ---- 1. Request line ----
        String requestLine = reader.readLine();
        // Be defensive: hand back a populated RequestInfo even for empty or
        // malformed input so the caller can never NPE on the getters.
        if (requestLine == null || requestLine.isEmpty()) {
            return new RequestInfo("", "", new String[0], new HashMap<>(), new byte[0]);
        }

        String[] parts = requestLine.split("\\s+");
        String httpCommand = parts.length > 0 ? parts[0] : "";
        String fullUri     = parts.length > 1 ? parts[1] : "";

        // ---- 2. URI + query string ----
        Map<String, String> parameters = new HashMap<>();
        String path = fullUri;
        int q = fullUri.indexOf('?');
        if (q >= 0) {
            path = fullUri.substring(0, q);
            parseQueryString(fullUri.substring(q + 1), parameters);
        }

        // path -> segments, dropping empty pieces caused by leading '/'
        String[] uriSegments = Arrays.stream(path.split("/"))
                                     .filter(s -> !s.isEmpty())
                                     .toArray(String[]::new);

        // ---- 3. Skip headers (up to first blank line) ----
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // We don't currently need any header values; tests don't check them.
        }

        // ---- 4. Form-style parameter lines (up to next blank line) ----
        // Use reader.ready() to avoid blocking on a real socket that has
        // sent everything and is just waiting for our response.
        while (reader.ready()) {
            line = reader.readLine();
            if (line == null || line.isEmpty()) break;
            int eq = line.indexOf('=');
            if (eq >= 0) {
                parameters.put(line.substring(0, eq).trim(),
                               line.substring(eq + 1).trim());
            }
        }

        // ---- 5. Content (up to next blank line, EOF, or no more data) ----
        StringBuilder content = new StringBuilder();
        while (reader.ready()) {
            line = reader.readLine();
            if (line == null || line.isEmpty()) break;
            content.append(line).append('\n');
        }

        return new RequestInfo(httpCommand, fullUri, uriSegments, parameters,
                               content.toString().getBytes());
    }

    private static void parseQueryString(String query, Map<String, String> out) {
        if (query == null || query.isEmpty()) return;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                out.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            } else if (!pair.isEmpty()) {
                out.put(pair.trim(), "");
            }
        }
    }

    /** Immutable bag of fields produced by {@link #parseRequest(BufferedReader)}. */
    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        /**
         * Constructs a {@code RequestInfo} from already-parsed pieces.
         *
         * @param httpCommand HTTP verb (e.g. {@code GET})
         * @param uri         full request URI including any query string
         * @param uriSegments path segments split on {@code '/'} with empties removed
         * @param parameters  combined map of query-string and form parameters
         * @param content     raw request body bytes
         */
        public RequestInfo(String httpCommand,
                           String uri,
                           String[] uriSegments,
                           Map<String, String> parameters,
                           byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        /**
         * Returns the HTTP verb.
         * @return the HTTP verb
         */
        public String getHttpCommand() { return httpCommand; }
        /**
         * Returns the full request URI.
         * @return the full request URI
         */
        public String getUri() { return uri; }
        /**
         * Returns the URI path segments.
         * @return URI path segments, empties removed
         */
        public String[] getUriSegments() { return uriSegments; }
        /**
         * Returns the merged parameter map.
         * @return the merged parameter map
         */
        public Map<String, String> getParameters() { return parameters; }
        /**
         * Returns the raw request body bytes.
         * @return the raw request body bytes
         */
        public byte[] getContent() { return content; }
    }
}
