import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Probe4 {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
        PackageFormat.Package pkg = PackageFormat.Package.Companion.getRootAsPackage(ByteBuffer.wrap(bytes));
        int n = pkg.getAllDeclsLength();
        System.out.println("file=" + args[0]);
        System.out.println("allDecls=" + n);
        int shown = 0;
        for (int i = 0; i < n && shown < 8; i++) {
            PackageFormat.Decl d = pkg.allDecls(i);
            String name = d.getIdentifier();
            if (name == null) name = d.getName();
            System.out.println("  decl[" + i + "] kind=" + d.getKind() + " name=" + name + " annos=" + d.getAnnotationsLength());
            shown++;
        }
    }
}
