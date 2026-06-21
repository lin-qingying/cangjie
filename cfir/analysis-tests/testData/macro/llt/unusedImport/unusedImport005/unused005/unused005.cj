// FILE: other.cj
macro package a

public import std.deriving.Derive
public import std.collection.ArrayList
import std.ast.*

public macro ModifyFunc(input: Tokens): Tokens {
    let funcDecl = FuncDecl(input)
    return quote(
        func $(funcDecl.identifier)() {
            println("start")
            $(funcDecl.block.nodes)
            println("end")
        })
}



// FILE: unused005.cj
import a.*

main() {
    var test : ArrayList<Int64>
}


