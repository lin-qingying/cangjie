// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {}

interface I1<T> {}

enum E<T> <: I {
    EE
}

enum E1<T> <: I1<Int32> {
    EE1
}

main(): Int64 {
    let a: I = <!UNABLE_TO_INFER_EXPR!>EE<!>
    let b: I1<Int32> = <!UNABLE_TO_INFER_EXPR!>EE1<!>
    let c: I1<Int64> = <!UNABLE_TO_INFER_EXPR!>EE1<!>
    let d: I1<Int64> = <!TYPE_MISMATCH!>EE1<Int64><!>
    return 1
}
