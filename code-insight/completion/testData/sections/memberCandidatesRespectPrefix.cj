package completion.sections

class Container {
    let visible: Int64 = 1
    let vacuum: Int64 = 2
}

func use(container: Container): Int64 {
    container.completionTarget
}
