package sample.patternprovider

public let (globalLeaf, sibling) = (7, true)

public func readLeaf(): Int64 {
    return globalLeaf
}
