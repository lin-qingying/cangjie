// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct B<X, Y> {
    public B(x: Int64, y: X, public var z: Y) {}
    public init(z: Y) {
        this.z = z
    }
}

struct C {
    public C(var z!: Int64 = 3) {
    }
}

main(): Int64 {
    B<Int64, Int64>(4).z
    if (B<Int64, Int64>(4).z == 4 && B<Int64, String>(1, 2, "str").z == "str" && C().z == 3) {
        return 0
    }
    return 1
}
