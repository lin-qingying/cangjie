// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: org1.cj
package org1::shared.lib
public func same(): Unit {}

// FILE: org2.cj
package org2::shared.lib
public func same(): Unit {}

// FILE: consumer.cj
package consumer
import org1::shared.lib.{same}
import org2::shared.lib.{same}
func consume(): Unit {
    same()
}
