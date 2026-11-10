import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Probe3 {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
        PackageFormat.Package pkg = PackageFormat.Package.Companion.getRootAsPackage(ByteBuffer.wrap(bytes));
        int n = pkg.getAllDeclsLength();
        int withAnno = 0, totalAnno = 0;
        for (int i = 0; i < n; i++) {
            PackageFormat.Decl d = pkg.allDecls(i);
            int c = d.getAnnotationsLength();
            if (c > 0) { withAnno++; totalAnno += c; }
        }
        System.out.println("pkg=" + pkg.getFullPkgName() + " decls=" + n + " withAnnotations=" + withAnno + " totalAnnotations=" + totalAnno);
    }
}
