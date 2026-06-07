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
    let b = <!sema_use_func_capture_var_alone!>g<!>
    <!sema_use_func_capture_var_alone!>g<!>
    g()
    func g2() {
        g()
    }
    let c = <!sema_func_capture_var_cannot_assign!>g<!>2
    func h() {
        print("${y}")
    }
    let d = h
    let e = <!sema_use_func_capture_var_alone!>{=> print("${x}")}<!>
    let i = {=> x * x}()
    return <!sema_use_func_capture_var_alone!>g<!>
}

let x = 4

func f2() {
    var a = x
    let x: Int64
    func f1() {
        <!sema_capture_before_initialization!>x<!>
    }
    x = 99
    f1()
}

main(): Int64 {
    return 0
}
