// LANGUAGE: +EffectHandlers
// FILE: effect_defs.cj
package stdx.effect

interface Command<T> {}
interface Resumption<T> {}

// FILE: main.cj
package effects

import stdx.effect.Command

class GoodCommand <: Command<Int64> {}
class BadCommand {}

func performBad(): Int64 {
    return perform <!COMMAND_INCOMPATIBLE_TYPE!>BadCommand()<!>
}

func handleTypeError(): Int64 {
    return try {
        1
    } handle (bad: <!COMMAND_HANDLE_TYPE_ERROR!>BadCommand<!>) {
        1
    }
}

func resumeNoWith(): Int64 {
    return try {
        1
    } handle (cmd: GoodCommand) {
        <!RESUME_NO_WITH!>resume<!>
    }
}

func resumeOutside(): Unit {
    <!IMPLICIT_RESUME_OUTSIDE_HANDLER!>resume<!>
}

func resumeThrowingType(): Int64 {
    return try {
        1
    } handle (cmd: GoodCommand) {
        <!RESUME_THROWING_MISMATCH_TYPE!>resume throwing 1<!>
    }
}

func mismatchHandleBlock(): Unit {
    try {
        1
    } handle (cmd: GoodCommand) {
        false
    }
}
