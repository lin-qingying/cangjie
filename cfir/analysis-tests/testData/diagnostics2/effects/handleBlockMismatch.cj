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
        <!UNUSED_EXPRESSION!>1<!>
    } handle (cmd: GoodCommand) <!MISMATCHING_HANDLE_BLOCK!>{
        false
    }<!>
}

func mismatchHandleBlockInBoolTry(): Unit {
    try {
        <!UNUSED_EXPRESSION!>true<!>
    } handle (cmd: GoodCommand) <!MISMATCHING_HANDLE_BLOCK!>{
        1
    }<!>
}
