import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Probe5 {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
        PackageFormat.Package pkg = PackageFormat.Package.Companion.getRootAsPackage(ByteBuffer.wrap(bytes));
        int n = pkg.getAllDeclsLength();
        int withAnno = 0, total = 0, named = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            PackageFormat.Decl d = pkg.allDecls(i);
            int c = d.getAnnotationsLength();
            if (c > 0) { withAnno++; total += c; }
            String id = d.getIdentifier();
            if (id != null) named++;
            if (i < 6 && sb.length() < 500) sb.append("[").append(i).append("] id=").append(id).append(" annos=").append(c).append(" ; ");
        }
        System.out.println("file=" + Paths.get(args[0]).getFileName());
        System.out.println("allDecls=" + n + " named=" + named + " withAnnotations=" + withAnno + " totalAnnotations=" + total);
        System.out.println("first: " + sb);
    }
}
