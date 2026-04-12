// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    public var a: Int64
    public var b: Int64
    init(a_: Int64, b_: Int64) {
        a = a_
        b = b_
    }
    operator func [](arg: A, value!: A) {
        this.a = arg.a
    }
}

func test1() {
    var a1 = 0
    var b1 = 0
    var f = {=> (1, 2)}
    (a1, b1) = f()
    if (a1 == 1 && b1 == 2) {
        return 0
    } else {
        return 1
    }
}

func test2() {
    var a2 = 0
    var b2 = 0
    var c2 = 0
    var d2 = 0
    var f = {=> ((1, 2, 3), 4)}
    ((a2, b2, c2), d2) = f()
    if (a2 == 1 && b2 == 2 && c2 == 3 && d2 == 4) {
        return 0
    } else {
        return 1
    }
}

func test3() {
    var a3 = 100
    var b3 = 200
    (a3, b3) = (b3, a3)
    if (a3 == 200 && b3 == 100) {
        return 0
    } else {
        return 1
    }
}

func test4() {
    var a1: A = A(1, 2)
    var a2: A = A(1, 2)
    (a1[a2], a2[a1]) = (a2, a1)
}

func test5() {
    var a5 = 0
    var b5 = 0
    var c5 = 0
    var f = {=> (1, 2, (3, 4, 5))}
    (a5, _, (b5, _, c5)) = f()
    if (a5 == 1 && b5 == 3 && c5 == 5) {
        return 0
    } else {
        return 1
    }
}

func test6() {
    var a6: (Int64, Int64)
    var b6: (Int64, String)
    (a6, b6) = ((1, 2), (3, "4"))
    if (a6[0] == 1 && a6[1] == 2 && b6[0] == 3 && b6[1] == "4") {
        return 0
    } else {
        return 1
    }
}

func test7() {
    var a7 = A(1, 2)
    (a7.a, a7.b) = (100, 200)
    if (a7.a == 100 && a7.b == 200) {
        return 0
    } else {
        return 1
    }
}

func test8() {
    var a = Some([0, 1])
    var b = Some([0, 1])
    (a?[0], b?[1]) = (3, 4)
    var res1 = match (a) {
        case Some(t) => t[0]
        case None => -1
    }
    var res2 = match (b) {
        case Some(t) => t[1]
        case None => -1
    }
    return res1 + res2 - 7
}

func test9() {
    var x: Int64 = 0
    (x, _) = (1, 2)
    if (x == 1) {
        return 0
    } else {
        return 1
    }
}

main() {
    test1() + test2() + test3() + test5() + test6() + test7() + test8() + test9()
}
