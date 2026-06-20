// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

class A <: I {}

func f(x: I) {}

main() {
    match (f) {
        case _: (A) -> Unit => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
}
