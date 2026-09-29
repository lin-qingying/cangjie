// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: provider.cj
package sample.lib

public class Box {}
public func make(): Unit {}

// FILE: consumer.cj
package consumer

import sample.lib.{Box, make as factory,}

func consume(): Unit {}
