// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(x: Int32): Float64 {
    10.0
}

func f(x: Int64): Int64 {
    -10
}

main() {
    let a = 2.0 <!INVALID_BINARY_OPERATOR!>**<!> f(2)
    0
}
