

enum Test<X> {
    Test(Int64, X)
  | Test(X)
  | Test(Bool, X, Int32)
}

main() {
    let a = Test<Rune>.Test('c')
    match (a) {
      case Test('c') => 1          // PC-Con
      case Test<Rune>('c') => 2    // PC-ConWithTyArgs
      case Test.Test('c) => 4       // PC-QualifiedConWithTyArgs
      case Test<Rune>.Test('c) => 3 // PC-QualifiedCon
    }
    return 0
}