// FILE: test2.cj


let (A, B) = (1, 2)

// FILE: test1.cj



// Test for using decl defined by varWithPatternDecl which is in the file the has bigger fileID than current fileID.
func test() {
    let a = A
}

main() {
    test()
}
