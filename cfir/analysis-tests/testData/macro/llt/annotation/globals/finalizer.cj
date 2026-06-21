

@Annotation[target: [MemberFunction]]
public class APILevel {
    public const init() {}
}

public class C {
    @APILevel
    ~init() {}
}

main() {}
