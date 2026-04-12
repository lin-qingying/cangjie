// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt --diagnostic-format=json %f 2>&1 | compare %f
// ASSERT: regex sema_lambdaExpr_must_have_type_annotation
// ASSERT: regex "Errors": 37

// should fail for op with no info
main() {
    {a => !a}
    {a => a + a}
    {a => a - a}
    {a => a * a}
    {a => a / a}
    {a => a % a}
    {a => a(a)}
    {a => a[a]}
    {a => a << a}
    {a => a >> a}
    {a => a > a}
    {a => a < a}
    {a => a >= a}
    {a => a <= a}
    {a => a == a}
    {a => a & a}
    {a => a ^ a}
    {a => a | a}
    {a, b => a + b}
    {a, b => a - b}
    {a, b => a * b}
    {a, b => a / b}
    {a, b => a % b}
    {a, b => a ** b}
    {a, b => a(b)}
    {a, b => a[b]}
    {a, b => a << b}
    {a, b => a >> b}
    {a, b => a > b}
    {a, b => a < b}
    {a, b => a >= b}
    {a, b => a <= b}
    {a, b => a == b}
    {a, b => a & b}
    {a, b => a ^ b}
    {a, b => a | b}
    {a, b, c, d => (a ^ (b & c)) | d}
    ()
}
