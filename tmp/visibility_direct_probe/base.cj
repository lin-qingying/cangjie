package visibilitydirect

private func hiddenTopLevel(): Int64 {
    return 1
}

open class Box {
    private func hiddenMember(): Int64 {
        return 2
    }

    func okInside(): Int64 {
        return hiddenMember()
    }
}
