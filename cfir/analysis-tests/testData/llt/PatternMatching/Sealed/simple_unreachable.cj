// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

sealed interface Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
        case _ => -1 // reachable
    }
}

main() {
    f(Red())
}
