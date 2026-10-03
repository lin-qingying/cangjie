// RUN_PIPELINE_TILL: FRONTEND
type Handler = CFunc<(Int64) -> Int32>
type HandlerArray = VArray<Handler, $4>
type BytePtr = CPointer<UInt8>
type CStr = CString
type Mapper = (Int64) -> String
type Pair = (Int64, String)
