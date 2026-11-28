package completion.expectedtype

class Alpha {}

func single(value: Alpha): Alpha {
    value
}

func recoversExpectedTypeFromSingleCandidate() {
    single(Alpha())
}
