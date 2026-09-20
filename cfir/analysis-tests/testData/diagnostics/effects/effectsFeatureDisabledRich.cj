class LocalCommand {}

func disabledPerform(): Unit {
    <!EFFECTS_FEATURE_DISABLED!>perform LocalCommand()<!>
}

func disabledResume(): Unit {
    <!EFFECTS_FEATURE_DISABLED!>resume<!>
}

func disabledHandle(): Int64 {
    return try {
        1
    } <!EFFECTS_FEATURE_DISABLED!>handle (cmd: LocalCommand) {
        1
    }<!>
}
