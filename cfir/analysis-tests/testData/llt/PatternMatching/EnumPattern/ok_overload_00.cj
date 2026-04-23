// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    | a(Int64)
    | a
}

func f(e: E) {
    match (e) {
        case E.a => 0
        case E.a(_) => 1
    }
}

main() {
    f(E.a)
}
