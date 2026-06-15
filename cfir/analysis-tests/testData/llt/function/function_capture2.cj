// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f() {
    var x = 1
    let y = 2
    func g() {
        print("${x}")
    }
    let b = <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    g()
    func g2() {
        g()
    }
    let c = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>g<!>2
    func h() {
        print("${y}")
    }
    let d = h
    let e = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> print("${x}")}<!>
    let i = {=> x * x}()
    return <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
}

let x = 4

func f2() {
    var a = x
    let x: Int64
    func f1() {
        <!CAPTURE_BEFORE_INITIALIZATION!>x<!>
    }
    x = 99
    f1()
}

main(): Int64 {
    return 0
}
