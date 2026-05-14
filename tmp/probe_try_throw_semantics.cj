class PlainClass {}
class Ex <: Exception {
    public init() {
        super()
    }
}

func throwPlain(): Unit {
    throw PlainClass()
}

func throwEx(): Unit {
    throw Ex()
}

func catchPlain(): Unit {
    try {
        throw Ex()
    } catch (e: PlainClass) {
        let _ = e
    }
}
