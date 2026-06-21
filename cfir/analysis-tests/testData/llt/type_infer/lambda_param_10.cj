


// should fail for op with no info
main() {
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => !a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a + a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a - a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a * a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a / a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a % a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a(a)}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a[a]}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a << a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a >> a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a > a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a < a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a >= a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a <= a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a == a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a & a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a ^ a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!> => a | a}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a + b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a - b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a * b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a / b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a % b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a ** b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a(b)}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a[b]}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a << b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a >> b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a > b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a < b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a >= b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a <= b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a == b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a & b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a ^ b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b => a | b}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>a<!>, b, c, d => (a ^ (b & c)) | d}
    ()
}
