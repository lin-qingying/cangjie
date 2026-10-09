// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: shadowed.cj
package completion.shadow

func shadowedByLocal(): Int64 {
    return 1
}

// FILE: provider.cj
package completion.shadow.provider

public func shadowedByLocal(): Int64 {
    return 2
}

public func plain(): Int64 {
    return 3
}

// FILE: consumer.cj
package completion.shadow

import completion.shadow.provider

class Holder {
    func memberCandidate(): Int64 {
        return 2
    }

    func useMember(): Int64 {
        return memberCandidate()
    }
}

func qualifiedShadowed(): Int64 {
    let shadowedByLocal = 4
    return provider.shadowedByLocal() + shadowedByLocal
}

func qualifiedPlain(): Int64 {
    return provider.plain()
}

func consume(): Int64 {
    let shadowedByLocal = 3
    return shadowedByLocal
}
