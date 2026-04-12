// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

abstract class FaTest {
    public var a: Int32 = 1
    public func f(): Int32
    public func g(): Int32 {
        return a * 3
    }
    public func getA(): Int32
    public func getA2(): Int32
}

class Test1 <: FaTest {
    public init(x: Int32) {
        a = x
    }
    public func f(): Int32 {
        return a * 2
    }
    public func getA(): Int32 {
        return this.f()
    }
    public func getA2(): Int32 {
        return super.g()
    }
}

class Test2 <: FaTest {
    public init(x: Int32) {
        a = x
    }
    public func f(): Int32 {
        return a
    }
    public func getA(): Int32 {
        return this.f()
    }
    public func getA2(): Int32 {
        return super.g()
    }
}

main() {
    var ins1 = Test1(5)
    var ins2 = Test2(10)

    var arr = [ins1, ins2]
    if (arr[0].getA() == 10 && arr[1].getA() == 10 && arr[0].getA2() == 15 && arr[1].getA2() == 30) {
        return 0
    }

    return 1
}

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace
