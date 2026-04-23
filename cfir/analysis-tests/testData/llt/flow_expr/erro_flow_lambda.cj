// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public open class A {}

func f(x: Int64): Int64 {
    1
}

main() {
    var d: Bool = 0 |> {
        f: Int64 =>
        <!TOO_MANY_ARGUMENTS!>A(name: 1)<!>;
        A()
    }
    var g = f ~> {
        a: Int64 =>
        <!TOO_MANY_ARGUMENTS!>A(name: 1)<!>;
        return 0
    }
}
