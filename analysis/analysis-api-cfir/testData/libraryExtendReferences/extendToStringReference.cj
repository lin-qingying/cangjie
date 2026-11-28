package sample.libraryextendreferences

interface Marker {

    func useSelf(): String

}

class Box {
}

extend Int8 <: Marker {

    public func useSelf(): String {
        return this.toString()
    }

}

extend ?Any <: Marker {

    public func useOptional(): String {
        return if (this.isNone()) {
            "none"
        } else {
            "some"
        }
    }

}

extend Box <: ToString {

    public func toString(): String {
        return "box"
    }

}

func useInt8(value: Int8): String {
    return value.toString()
}
