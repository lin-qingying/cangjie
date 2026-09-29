// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: org1.cj
package org1::shared

public class Box {}
public func same(): Unit {}

// FILE: org2.cj
package org2::shared

public class Box {}
public func same(): Unit {}

// FILE: unqualified.cj
package shared

public class Box {}
public func same(): Unit {}

// FILE: consumer.cj
package consumer

import org1::shared.{same as first, Box as FirstBox}
import org2::shared.{same as second, Box as SecondBox}

func consume(): Unit {}
