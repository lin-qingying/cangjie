package completion.expectedparametertype

class Alpha {}

func takes(value: Alpha): Alpha {
    value
}

func unknownNamedArgumentYieldsNoExpectedType() {
    takes(missing: Alpha())
}
