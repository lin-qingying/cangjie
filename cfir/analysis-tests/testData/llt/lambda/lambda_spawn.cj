// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func benchmark(): Unit {
    let f: (Int64) -> Unit
    f = {
        n: Int64 => var res = spawn {
            => <!CAPTURE_BEFORE_INITIALIZATION!>f<!>(n - 1)
        }
    }
    f(4)
}

main() {
    benchmark()
}
