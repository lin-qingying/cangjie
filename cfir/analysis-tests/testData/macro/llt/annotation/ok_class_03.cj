

@Annotation
public class A0 {
    public const init() {
        let _ = C0()
    }
}

public class C0 {
    @A0
    public const init() {}
}

main() {}
