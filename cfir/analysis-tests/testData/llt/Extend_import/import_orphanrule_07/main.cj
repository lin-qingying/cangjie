// FILE: p1.cj


package p1

public interface Strbase <: Collection<Byte> & Comparable<String> & Hashable & ToString {}

extend String <: Strbase {}

// FILE: main.cj



internal import p1.*

main(): Int64 {
    0
}
