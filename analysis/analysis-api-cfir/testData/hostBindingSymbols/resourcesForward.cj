package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
}

func useForwardResources(): Unit {
    let <caret_outerDeclaration>later = ManagedResource()
    try (first: ManagedResource = <caret_forward>later, later: ManagedResource = <caret_selfInitializer>later) {
        let one = <caret_firstUse>first
        let two = <caret_laterUse>later
    } catch (failure: Exception) {
        let unavailable = <caret_catchUnavailable>first
        let outer = <caret_catchOuter>later
    } finally {
        let unavailable = <caret_finallyUnavailable>first
        let outer = <caret_finallyOuter>later
    }
    let unavailable = <caret_afterUnavailable>first
    let outer = <caret_afterOuter>later
}

func resourceAndBodyShareScope(): Unit {
    try (same = ManagedResource()) {
        let <caret_duplicateDeclaration>same = ManagedResource()
    }
}
