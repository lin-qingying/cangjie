

public open class A {}

abstract sealed class C <: A {}

public class C1 <: C {}

public class C2 <: C {}

public class C3 <: C {}

func f1(obj: C) {
    match (obj) {
        case v1: C1 => 0
        case v2: C2 => 1
        case v3: A => 2
    }
}

main() {}
