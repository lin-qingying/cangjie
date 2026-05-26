// RUN_PIPELINE_TILL: FRONTEND
// FILE: lib.cj
package sample.lib

public type RemoteAlias = Int64

public func echo(value: RemoteAlias): RemoteAlias {
    return value
}

// FILE: main.cj
package sample.main

import sample.lib.RemoteAlias
import sample.lib.echo

func useAlias(value: RemoteAlias): RemoteAlias {
    return echo(value)
}
