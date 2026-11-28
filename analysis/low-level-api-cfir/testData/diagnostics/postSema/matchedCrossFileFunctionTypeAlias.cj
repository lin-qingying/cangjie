// MODULE: main
// MAIN_FILE_NAME: matchedCrossFileFunctionTypeAlias.cj
// FILE: handlerTypes.cj
package test

type Handler = (Int64) -> Unit

// FILE: matchedCrossFileFunctionTypeAlias.cj
package test

import std.collection.*

func invokeCrossFileMap(map: HashMap<String, Handler>): Unit {
    match (map.get("key")) {
        case Some(handler) => handler(1)
        case None => ()
    }
}
