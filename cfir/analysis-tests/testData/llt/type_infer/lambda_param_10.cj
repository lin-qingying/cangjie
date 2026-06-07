// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// should fail for op with no info
main() {
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => !a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a + a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a - a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a * a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a / a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a % a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a(a)}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a[a]}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a << a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a >> a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a > a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a < a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a >= a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a <= a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a == a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a & a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a ^ a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!> => a | a}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a + b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a - b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a * b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a / b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a % b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a ** b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a(b)}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a[b]}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a << b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a >> b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a > b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a < b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a >= b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a <= b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a == b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a & b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a ^ b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b => a | b}
    {<!sema_lambdaExpr_must_have_type_annotation!>a<!>, b, c, d => (a ^ (b & c)) | d}
    ()
}
