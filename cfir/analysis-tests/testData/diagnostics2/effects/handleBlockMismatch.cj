// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class GoodCommand <: Command<Int64> {}

func mismatchHandleBlockInIntTry(): Unit {
    try {
        1
    } <!MISMATCHING_HANDLE_BLOCK!>handle (cmd: GoodCommand) {
        false
    }<!>
}

func mismatchHandleBlockInBoolTry(): Unit {
    try {
        true
    } <!MISMATCHING_HANDLE_BLOCK!>handle (cmd: GoodCommand) {
        1
    }<!>
}
