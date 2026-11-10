import java.nio.ByteBuffer;
import java.nio.file.*;

public class ProbeAll {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        int files = 0, decls = 0, withAnno = 0, total = 0;
        java.util.List<Path> cjos = new java.util.ArrayList<>();
        Files.walk(root).filter(p -> p.toString().endsWith(".cjo")).forEach(cjos::add);
        for (Path p : cjos) {
            try {
                byte[] bytes = Files.readAllBytes(p);
                PackageFormat.Package pkg = PackageFormat.Package.Companion.getRootAsPackage(ByteBuffer.wrap(bytes));
                files++;
                int n = pkg.getAllDeclsLength();
                decls += n;
                for (int i = 0; i < n; i++) {
                    int c = pkg.allDecls(i).getAnnotationsLength();
                    if (c > 0) { withAnno++; total += c; }
                }
            } catch (Throwable t) {
                System.out.println("skip " + p.getFileName() + ": " + t);
            }
        }
        System.out.println("files=" + files + " decls=" + decls + " withAnnotations=" + withAnno + " totalAnnotations=" + total);
    }
}
