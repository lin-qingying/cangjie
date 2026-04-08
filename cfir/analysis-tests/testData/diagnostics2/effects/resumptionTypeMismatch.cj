// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class BoolCommand <: Command<Bool> {}
class IntCommand <: Command<Int64> {}

func wrongResumeValue(): Int64 {
    return try {
        1
    } handle (cmd: IntCommand) {
        <!TYPE_MISMATCH!>resume with false<!>
    }
}

func wrongResumeValueForBool(): Bool {
    return try {
        true
    } handle (cmd: BoolCommand) {
        <!TYPE_MISMATCH!>resume with 1<!>
    }
}
