package completion.sections

@Deprecated
public func targetLegacy(): Int64 { 1 }

public func targetFresh(): Int64 { 2 }

func deprecatedSite(): Int64 {
    target
}
