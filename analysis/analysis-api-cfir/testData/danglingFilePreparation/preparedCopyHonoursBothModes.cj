package sample.danglingPreparation

func localHelper(): Int64 {
    1
}

func consumeIt(): Int64 {
    return localHelper()
}
