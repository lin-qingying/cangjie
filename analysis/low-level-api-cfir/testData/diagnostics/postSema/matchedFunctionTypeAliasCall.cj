package test

type Handler = (Int64) -> Unit

func invokeMatchedHandler(opt: ?Handler): Unit {
    match (opt) {
        case Some(handler) => handler(1)
        case None => ()
    }
}
