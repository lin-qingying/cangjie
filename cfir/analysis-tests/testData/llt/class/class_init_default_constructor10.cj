

open class Tree {
    var left: Tree = Tree_None()
    var right: Tree = Tree_None()
    init(left: Tree, right: Tree) {
        this.left = left
        this.right = right
    }
}

class <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>Tree_None<!> <: Tree {}
