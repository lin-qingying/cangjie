// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f
// EXEC: %compiler %cmp_opt %f -O2

func foo(c: Int) {
    if (let _ <- c) {
        println(c)
    }
}

func bar(c: Int) {
    if (let _ | _ <- c) {
        println(c)
    }
}

main() {
    foo(3)
    bar(7)
}