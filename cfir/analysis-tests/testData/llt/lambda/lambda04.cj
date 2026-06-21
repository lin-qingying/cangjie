


func A() {
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!> => i}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!>, j => i + j}
}

main() {
    return 0
}
