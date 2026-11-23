package test.patterns.pointers

let (<caret_doomed>doomed, <caret_spare>spare) = (1, 2)

func survivingLocal(): Int64 {
    let (<caret_shadowDoomed>doomed, <caret_shadowSpare>spare) = (3, 4)
    return doomed + spare
}

main() { println(survivingLocal()) }
