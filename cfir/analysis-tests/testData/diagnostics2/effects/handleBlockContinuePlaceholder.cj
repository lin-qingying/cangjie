// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class IntCommand <: Command<Int64> {}

func continueFromHandle(): Int64 {
    <!RETURN_TYPE_MISMATCH!>while (true) {
        return try {
            1
        } handle (cmd: IntCommand) {
            <!INVALID_LOOP_CONTROL!>continue<!>
        }
    }<!>
}
