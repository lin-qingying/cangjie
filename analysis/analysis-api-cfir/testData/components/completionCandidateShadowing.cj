// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: shadowed.cj
package completion.shadow

func shadowedByLocal(): Int64 {
    return 1
}

// FILE: consumer.cj
package completion.shadow

class Holder {
    func memberCandidate(): Int64 {
        return 2
    }

    func useMember(): Int64 {
        return memberCandidate()
    }
}

func consume(): Int64 {
    let shadowedByLocal = 3
    return shadowedByLocal
}
