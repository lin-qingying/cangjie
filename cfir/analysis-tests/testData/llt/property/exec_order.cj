// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

class A {
    mut prop m: Int64 {
        get() {
            println("a=${a}")
            a
        }
        set(value) {
            println("a=${a} value=${value}")
            a = value
        }
    }
    var a = 1

    mut static prop stm: Int64 {
        get() {
            println("sta=${sta}")
            sta
        }
        set(value) {
            println("sta=${sta} value=${value}")
            sta = value
        }
    }
    static var sta = 3
}

var a = A()

func geta(n: String) {
    println(n)
    a
}

var b = VArray<Int64, $3>({i => i})

func getb(n: String) {
    println(n)
    b
}

main() {
    geta("left").m = geta("right").m + 1
    geta("left").m += geta("right").m
    A.stm = A.stm + 2
    A.stm += 2
}

/* SCAN
right
a=1
left
a=1 value=2
left
a=2
right
a=2
a=2 value=4
sta=3
sta=3 value=5
sta=5
sta=5 value=7
 */
