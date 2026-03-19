// RUN_PIPELINE_TILL: FRONTEND
type FixedStrings = VArray<String, $4>
type HandlerTable = VArray<(Int64) -> Bool, $2>

func copy(src: VArray<String, $4>): VArray<String, $4> {
    return src
}

func useHandlers(handlers: VArray<(Int64) -> Bool, $2>): HandlerTable {
    return handlers
}

