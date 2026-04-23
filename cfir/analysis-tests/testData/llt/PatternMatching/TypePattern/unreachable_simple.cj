// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(x: Int64) {
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: Bool<!> => -1 // unreachable
        case _ => 0
        case <!UNREACHABLE_PATTERN!>_: ToString<!> => -1 // unreachable
    }
}

main() {
    f(0)
}
