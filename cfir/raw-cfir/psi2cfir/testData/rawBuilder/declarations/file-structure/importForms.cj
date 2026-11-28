// RUN_PIPELINE_TILL: FRONTEND
package importforms

import std.collection.*
import std.collection.ArrayList
import std.collection as col

func useImports(): Unit {
    let a = ArrayList<Int64>()
    let b = col.ArrayList<Int64>()
    let _ = a
    let _ = b
}
