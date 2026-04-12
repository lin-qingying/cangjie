// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class AAAA {
    public var publicAvar: Int32 = 1
    public init(a: Int32) {
        this.publicAvar = a
    }

    public init() {
        this.publicAvar = 100
    }

    public init(a: Int32, b: Int32) {
        this.publicAvar = a + b
    }

    public func publicAAAfunc(a: Int32) {
        return a
    }

    public func publicAAAfunc(a: Int32, b: Int32) {
        return a + b
    }

    public func publicAAAfunc() {
        return 100
    }
}

main() {
    var valueA: AAAA = AAAA(1)
    var a = valueA.publicAAAfunc()
    var b = valueA.publicAAAfunc(1)
    var c = valueA.publicAAAfunc(2)
    var d = valueA.publicAAAfunc(1, 2)
    var e = valueA.publicAAAfunc(2, 3)
    var f = valueA.publicAAAfunc(3, 4)
    return a + Int64(b) + Int64(c) + Int64(d) + Int64(e) + Int64(f) - 118
}
