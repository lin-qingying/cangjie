// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: org1.cj
package org1::shared

public func same(): Unit {}

// FILE: org2.cj
package org2::shared

public func same(): Unit {}

// FILE: unqualified.cj
package shared

public func same(): Unit {}

// FILE: consumer.cj
package consumer

import org1::shared.*
import org2::shared as second

func consume(): Unit {
    same()
    second.same()
}
