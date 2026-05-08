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
        <!RETURN_IN_TRY_HANDLE_BLOCK!>return 2<!>
    }
}
