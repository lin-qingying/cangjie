// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    prop a: Int64
}

class RR <: I {
    public var aa: Int64 = 98
    public prop a: Int64 {
        get() {
            return aa
        }
    }
}

class R {
    public var a: I = RR()
}

main() {
    var b = R()
    print("${b.a.a}")
    return b.a.a - 98
}
