// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class GoodCommand <: Command<Int64> {}

func mismatchHandleBlock(): (Int64, Bool) {
    return <!MISMATCHING_HANDLE_BLOCK!>try {
        (1, true)
    } <!MISMATCHING_HANDLE_BLOCK!>handle (cmd: GoodCommand) {
        1
    }<!><!>
}
