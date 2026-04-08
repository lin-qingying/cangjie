// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class IntCommand <: Command<Int64> {}

func breakFromHandle(): Int64 {
    while (true) {
        return try {
            1
        } handle (cmd: IntCommand) {
            <!INVALID_LOOP_CONTROL!>/* suggested message: loop control cannot escape directly from a handle block boundary. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */break<!>
        }
    }
}
