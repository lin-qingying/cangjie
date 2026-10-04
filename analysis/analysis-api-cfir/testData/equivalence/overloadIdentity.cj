package sample.symbol.overload

class Document {
    func render(): Int64 {
        return 1
    }

    func render(width: Int64): Int64 {
        return width
    }
}

extend Document {
    func describe(): Int64 {
        return 1
    }

    func describe(width: Int64): Int64 {
        return width
    }
}

func format(value: Int64): Int64 {
    return value
}

func format(value: Int64, width: Int64): Int64 {
    return value
}

main(): Int64 {
    let document = Document()
    return format(document.render()) + document.describe() + format(document.render(1))
}