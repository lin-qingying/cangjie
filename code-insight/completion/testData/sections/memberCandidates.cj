package completion.sections

class Container {
    private var hidden: Int64 = 0
    public let visible: Int64 = 1

    public func reveal(): Int64 {
        visible
    }
}

func use(container: Container): Int64 {
    container.completionTarget
}
