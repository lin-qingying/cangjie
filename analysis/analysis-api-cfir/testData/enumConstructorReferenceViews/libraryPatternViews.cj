public func probeA(): ?UInt64 {
    match (Some(1)) {
        case None1 => return None
        case So<caret_somePattern>me(v) => 1
    }
}

public func probeB(): ?UInt64 {
    match (Some(1)) {
        case No<caret_nonePattern>ne => return None
        case So<caret_some1>me1(v) => 1
    }
}

public func probeC(): ?UInt64 {
    match (Some(1)) {
        case None => return None
        case So<caret_som>m(v) => 1
    }
}

main(): Int64 {
    return 0
}
