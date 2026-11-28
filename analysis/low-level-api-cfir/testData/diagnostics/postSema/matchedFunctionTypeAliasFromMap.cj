package test

import std.collection.*

type Handler = (Int64) -> Unit

func invokeFromMap(map: HashMap<String, Handler>): Unit {
    match (map.get("key")) {
        case Some(handler) => handler(1)
        case None => ()
    }
}
