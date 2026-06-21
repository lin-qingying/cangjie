// FILE: pkg.cj


package pkg

sealed interface Interaction {}

public class Weak <: Interaction {}

public class Strong <: Interaction {}

public class Gravity {}

public class Electromagnetic <: Interaction {}

extend Gravity <: Interaction {}

// FILE: fail.cj



internal import pkg.*

func mediator(f: Interaction) {
    <!NON_EXHAUSTIVE_MATCH!>match (f) {
        case _: Weak => "W and Z bosons"
        case _: Strong => "gluons"
        // case _: Gravity => "gravitons"
        case _: Electromagnetic => "photons"
    }<!>
}

main() {
    println(mediator(Gravity()))
}
