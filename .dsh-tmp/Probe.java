import PackageFormat.*;
import java.nio.*;
import java.nio.file.*;

public class Probe {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
        Package pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes));
        int n = pkg.getAllDeclsLength();
        int withAnno = 0, totalAnno = 0;
        StringBuilder samples = new StringBuilder();
        for (int i = 0; i < n; i++) {
            Decl d = pkg.allDecls(i);
            int c = d.getAnnotationsLength();
            if (c > 0) {
                withAnno++;
                totalAnno += c;
                if (samples.length() < 400) {
                    Anno a = d.annotations(0);
                    samples.append("decl#").append(i).append(" kind=").append(a.kind())
                           .append(" id=").append(a.identifier()).append(" ; ");
                }
            }
        }
        System.out.println("pkg=" + pkg.getFullPkgName() + " decls=" + n + " withAnnotations=" + withAnno + " totalAnnotations=" + totalAnno);
        System.out.println("samples: " + samples);
    }
}
