public func probeA(): ?UInt64 {
    match (Some(1)) {
        case None1 => return None
        case Some(v) => 1
    }
}

public func probeB(): ?UInt64 {
    match (Some(1)) {
        case None => return None
        case Some1(v) => 1
    }
}

public func probeC(): ?UInt64 {
    match (Some(1)) {
        case None => return None
        case Som(v) => 1
    }
}

main(): Int64 {
    return 0
}
