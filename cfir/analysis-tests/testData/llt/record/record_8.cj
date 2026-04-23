// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    private static var x: Int64 = 0
    public mut static prop a: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

main() {
    var test = A()
    A.a = 4
    var b = A.a
    if (b == 4) {
        print("${A.a}")
        return 0
    }
    return 1
}
