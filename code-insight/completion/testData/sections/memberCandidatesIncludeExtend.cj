package completion.sections

class Container {}

extend Container {
    func extended(): Int64 {
        1
    }
}

func use(container: Container): Int64 {
    container.completionTarget
}
