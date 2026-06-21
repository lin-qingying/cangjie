

//    Symbol
//      /\
//     B  H
//    /\   \
//   A  E   G
//  /\     /
// C  F   D
// src classdecl order: H A C D E B F G
// dst classdecl order: H G D, B A C E F

public interface Symbol <: Equatable<Symbol> {
    operator override func ==(that: Symbol): Bool {
        return true
    }

    operator override func !=(that: Symbol): Bool {
        return !(this == that)
    }
}

public abstract class H <: Symbol {}

public interface MemberSymbol <: Symbol {}

public abstract class A <: B & MemberSymbol {}

public open class C <: A {}

public open class D <: G & MemberSymbol {}

public open class E <: B {}

public abstract class B <: Symbol {}

public open class F <: A {}

public open class G <: H {}

main() {
    0
}
