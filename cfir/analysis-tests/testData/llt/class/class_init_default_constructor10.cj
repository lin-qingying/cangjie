

open class Tree {
    var left: Tree = Tree_None()
    var right: Tree = Tree_None()
    init(left: Tree, right: Tree) {
        this.left = left
        this.right = right
    }
}

<!EXPLICIT_SUPER_CALL_REQUIRED!>class Tree_None <: Tree {}<!>
