package test/* RootStructureElement */

class Host {/* ClassDeclarationStructureElement */
}

abstract class Box<T> {/* ClassDeclarationStructureElement */
    public func unwrap(): T {/* DeclarationStructureElement */
        return value
    }

    public prop value: T/* DeclarationStructureElement */
}

extend <T> Box<T> {/* ExtendDeclarationStructureElement */
    public func tag(): String {/* DeclarationStructureElement */
        return this.toString()
    }

    public prop label: String/* DeclarationStructureElement */ {
        get() {
            return "box"
        }
    }
}
