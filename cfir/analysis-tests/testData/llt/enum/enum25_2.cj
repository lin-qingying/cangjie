// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum A<T> {
    a(T, T, T)

    public prop c: Int64 {
        get() {
            1
        }
    }
    public func b() {}
    public func b(x: Int64, y: Int64, z: Int64) {}
    public func b(x: Int64) {}
    public func b(x: Float32) {}
}

main(): Int64 {
    var i = A.a(1, 2, 3)
    return i.c - 1
}
