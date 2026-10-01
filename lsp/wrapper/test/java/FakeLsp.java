import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.nio.charset.StandardCharsets;

/**
 * Minimal fake LSP server used to verify the Rust shim:
 * 1. reads LSP headers + body from stdin (raw bytes, no buffering)
 * 2. replies to "initialize" and "shutdown" on stdout
 * 3. exits cleanly on "exit" / EOF
 *
 * Writes to raw stdout (no buffering) as a real Java LSP must.
 */
public class FakeLsp {
    private static OutputStream out;
    private static InputStream in;

    public static void main(String[] args) throws Exception {
        System.err.println("FakeLsp started, args=" + String.join(" ", args));
        out = new FileOutputStream(FileDescriptor.out);
        in = System.in;

        while (true) {
            String json = readMessage();
            if (json == null) {
                System.err.println("FakeLsp EOF");
                return;
            }
            if (json.contains("\"method\":\"initialize\"")) {
                send("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"capabilities\":{\"textDocumentSync\":1}}}");
            } else if (json.contains("\"method\":\"shutdown\"")) {
                send("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":null}");
            } else if (json.contains("\"method\":\"exit\"")) {
                System.err.println("FakeLsp exit");
                return;
            }
        }
    }

    /** Reads one LSP message: headers terminated by \r\n\r\n, then body. */
    private static String readMessage() throws Exception {
        StringBuilder header = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            header.append((char) c);
            int len = header.length();
            if (len >= 4
                    && header.charAt(len - 1) == '\n'
                    && header.charAt(len - 2) == '\r'
                    && header.charAt(len - 3) == '\n'
                    && header.charAt(len - 4) == '\r') {
                break;
            }
        }
        if (c == -1 && header.length() == 0) {
            return null;
        }
        int contentLength = -1;
        for (String line : header.toString().split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }
        if (contentLength < 0) {
            return "";
        }
        byte[] body = new byte[contentLength];
        int read = 0;
        while (read < contentLength) {
            int n = in.read(body, read, contentLength - read);
            if (n < 0) {
                return null;
            }
            read += n;
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static void send(String json) throws Exception {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String head = "Content-Length: " + body.length + "\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }
}
