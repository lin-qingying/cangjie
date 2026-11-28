package sample.scopecontext

interface Marker {

    func marker(): Unit

}

class Box {
}

extend Box <: Marker {

    public func marker(): Unit {
    }

    public func useSelf(): String {
        return this.toString()
    }

}

func caller() {
    let box = Box()
    box.useSelf()
}
