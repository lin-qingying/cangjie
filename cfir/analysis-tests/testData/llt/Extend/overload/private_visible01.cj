

// Original case.
open class Klass {
    func Check(arg: Int8) {
    }
}

extend Klass {
    private func h() {}
}

extend Klass {
    private static func h(a: Int64) {} // OK
}

main(ina: Array<String>) {
    var kl: Klass
    0
}
