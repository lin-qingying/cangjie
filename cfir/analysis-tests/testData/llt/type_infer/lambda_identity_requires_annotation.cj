let globalIdentity = { <!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>x<!> => x }
main() {
    let localIdentity = { <!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>x<!> => x }
    println(globalIdentity(Int64(1)))
    println(localIdentity(Int64(1)))
    return 0
}
