package sample.hints

class Box {
    func first(): Box {
        return this
    }

    func second(): Box {
        return this
    }
}

func demo(box: Box)<#: Box#> {
    box
        .first()<#: Box#>
        .second()<#: Box#>
}
