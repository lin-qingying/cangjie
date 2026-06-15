// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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
