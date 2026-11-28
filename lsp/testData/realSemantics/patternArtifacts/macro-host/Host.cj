package sample.patternmacro

import patternmacros.EmitGlobals

let hostAnchor: Int64 = 40

@EmitGlobals
public func carrier(): Unit {}

public func readCount(): Int64 {
    return generatedCount
}

public func readLabel(): String {
    return generatedLabel
}

public func readFlag(): Bool {
    return generatedFlag
}
