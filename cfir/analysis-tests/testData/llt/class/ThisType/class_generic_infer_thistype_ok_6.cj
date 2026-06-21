

func arrayify<T>(body: () -> T): () -> Array<T> {
    {=> [body()]}
}

class Test {
    func self(): This {
        this
    }
    func selfArray() {
        arrayify(self)
    }
}

main(): Int64 {
    let arr = Test().selfArray()()
    if (arr.size != 1) {
        return 1
    }
    return 0
}
