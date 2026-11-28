package g4consumer

private import g4provider.localForeignValue

public func useForeignDirectExtension(): Int64 {
    let value: Int64 = localForeignValue()
    value.foreignValue()
}
