struct StructChoice {
    func choose(value: ?Int64) {
        if (let Some(item) <- value) { return choose(item) }
        return 0
    }
    func choose(value: Int64) { 1 }
}

enum EnumChoice {
    Entry
    func choose(value: ?Int64) {
        if (let Some(item) <- value) { return choose(item) }
        return 0
    }
    func choose(value: Int64) { 1 }
}

interface InterfaceChoice {
    func choose(value: ?Int64) {
        if (let Some(item) <- value) { return choose(item) }
        return 0
    }
    func choose(value: Int64) { 1 }
}

class Implementation <: InterfaceChoice {}

main() {
    let fromStruct: Int64 = StructChoice().choose(Some(1))
    let fromEnum: Int64 = EnumChoice.Entry.choose(Some(1))
    let fromInterface: Int64 = Implementation().choose(Some(1))
    fromStruct + fromEnum + fromInterface - 3
}
