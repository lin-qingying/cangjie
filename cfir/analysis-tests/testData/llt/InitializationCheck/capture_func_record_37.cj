// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

var res: Int32 = 0

struct Aoo {
    public static var a: Int32 = 1
    public static let b: Int32 = 1
    public var c = if (true) {
        func foo1(x!: Int32 = a) {
            return x
        }
        func foo2() {
            var x = a;
            return x
        }
        func foo3() {
            return a
        }
        func foo4() {
            a;
            return Int32(1)
        }
        func foo5() {
            a
        }

        func foo10(x!: Int32 = b) {
            return x
        }
        func foo20() {
            var x = b;
            return x
        }
        func foo30() {
            return b
        }
        func foo40() {
            b;
            return Int32(1)
        }
        func foo50() {
            b
        }

        func goo1() {
            var a = foo1
            var b = foo10
            res += a(1)
            res += b(1)
        }
        func goo2() {
            var a = foo2
            var b = foo20
            res += a()
            res += b()
        }
        func goo3() {
            var a = foo3
            var b = foo30
            res += a()
            res += b()
        }
        func goo4() {
            var a = foo4
            var b = foo40
            res += a()
            res += b()
        }
        func goo5() {
            var a = foo5
            var b = foo50
            res += a()
            res += b()
        }
        goo1()
        goo2()
        goo3()
        goo4()
        goo5()
    }
}

main(): Int64 {
    var a = Aoo()
    if (res != 10) {
        return 1
    }
    return 0
}
