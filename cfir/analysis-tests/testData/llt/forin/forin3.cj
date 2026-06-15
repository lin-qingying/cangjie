// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func case01() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    let b: Int32 = 2
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (2 in intList) {
        a = b
    }
    a
}

func case03() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (_: Int32 in intList) {
        a++
    }
    a
}

enum TimeUnit {
    Year(Int32)
    | Mounth
}

main() {
    let intList: Array<TimeUnit>
    let a: Array<TimeUnit> = [Year(1), Year(2)]
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (Year(x) in a) {
    }
    0
}
