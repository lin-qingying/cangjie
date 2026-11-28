package test

type Handler = (Int64) -> Unit

func invokeHandler(handler: Handler): Unit {
    handler(1)
}
