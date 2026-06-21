

import std.deriving.*

@Annotation
public class APILevel {
    public const init() {}
}

@Derive[Equatable]
public enum Battery {
    @APILevel[]
    NONE
    | ENABLE
}

main() {}
