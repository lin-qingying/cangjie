// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class IntCommand <: Command<Int64> {}

func returnFromHandle(): Int64 {
    return try {
        1
    } handle (cmd: IntCommand) {
        <!RETURN_IN_HANDLE_BLOCK!>/* suggested message: return is not allowed directly from a handle block in this semantic context. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */return 2<!>
    }
}
