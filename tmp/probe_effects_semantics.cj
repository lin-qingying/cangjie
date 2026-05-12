import stdx.effect.Command

class GoodCommand <: Command<Int64> {}
class BadCommand {}

func performBad(): Int64 {
    return perform BadCommand()
}

func handleTypeError(): Int64 {
    return try {
        1
    } handle (bad: BadCommand) {
        1
    }
}

func resumeNoWith(): Int64 {
    return try {
        1
    } handle (cmd: GoodCommand) {
        resume
    }
}

func resumeOutside(): Unit {
    resume
}

func resumeThrowingType(): Int64 {
    return try {
        1
    } handle (cmd: GoodCommand) {
        resume throwing 1
    }
}

func mismatchHandleBlock(): Unit {
    try {
        1
    } handle (cmd: GoodCommand) {
        false
    }
}

func mismatchHandleBlockValue(): (Int64, Bool) {
    return try {
        (1, true)
    } handle (cmd: GoodCommand) {
        1
    }
}
