package completion.expectedtype

class Alpha {}

func noCandidateYieldsNoExpectedType() {
    missingFunction(Alpha())
}
