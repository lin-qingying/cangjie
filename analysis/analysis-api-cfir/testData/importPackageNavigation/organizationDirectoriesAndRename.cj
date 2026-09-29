// MODULE: main
// PHYSICAL_SOURCE_FILES
// MAIN_FILE_NAME: consumer.cj
// FILE: org1a/Box.cj
package org1::shared.lib
public class Box {}

// FILE: org1b/Other.cj
package org1::shared.lib
public class Other {}

// FILE: org2/Box.cj
package org2::shared.lib
public class Box {}

// FILE: renamed/Box.cj
package org1::shared.changed
public class Box {}

// FILE: consumer.cj
package consumer
import org1::shared.lib.{Box as First}
import org2::shared.lib.{Box as Second}
func consume(): Unit {}
