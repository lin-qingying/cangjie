package sample.targets.rich

interface Printable {
    func prettyPrint(): Int64
}

class Document {
}

class Holder {
    prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }

    init(seed: Int64) {
    }

    ~init() {
    }
}

extend Document <: Printable {
    func prettyPrint(): Int64 {
        return 42
    }
}

func useTargets(doc: Document, holder: Holder): Int64 {
    let current = holder.state
    return doc.prettyPrint() + current
}
