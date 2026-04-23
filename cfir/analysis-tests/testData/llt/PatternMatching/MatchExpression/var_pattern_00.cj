// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    var x = 0
}

func g(x: Int64) {
    x
}

func h(x: Rune) {
    A()
}

main() {
    g ~> {
        x: Int64 => h(match (123) {
            case xxx => r'n'
        }).x += 1
    }
    ()
}
