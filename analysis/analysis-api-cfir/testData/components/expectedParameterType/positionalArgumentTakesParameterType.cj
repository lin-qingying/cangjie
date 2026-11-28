package completion.expectedparametertype

class Alpha {}

func takes(value: Alpha): Alpha {
    value
}

func positionalArgumentTakesParameterType() {
    takes(Alpha())
}
