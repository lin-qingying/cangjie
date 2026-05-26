// MODULE: dep
// FILE: dep.cj
package peers.dep

public func helper(): Int64 {
    return 1
}

// MODULE: main(dep)
// FILE: main.cj
package peers.main

import peers.dep.*

func compute(): Int64 {
    return helper()
}
