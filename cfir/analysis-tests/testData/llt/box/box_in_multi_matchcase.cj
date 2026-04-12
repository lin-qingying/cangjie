// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func f(value: Any): Unit {
    match (value) {
        case x: Int8 => match (value) {
            case v: Int8 => Int64(v)
            case _ => throw Exception()
        }
        case _ => throw Exception()
    }
}

main() {
    f(Int8(1))
    0
}
