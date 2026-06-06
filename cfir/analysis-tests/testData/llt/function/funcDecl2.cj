// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func test() {}

func test1(a!: Int32 = 2, b!: Int32 = 2) {}

func help() {
    return 0
}

func help2(): Unit {
    return <!sema_cannot_convert_literal!>0<!>
}

func help3(): Unit {
    return
}

func exit() {
    if (true) {
        return 0
    } else {
        return ""
    };
    <!sema_incompatible_func_body_and_return_type!>r<!>eturn
}
//func exit2() {if (true {return 0} else) {}}
func exit3() {
    if (true) {
        return
    } else {
        return
    };
    return
}

main() {
    test1(a: 1, b: 1)
    test1(<!NEED_NAMED_ARGUMENT!>1<!>, b: 1)
    test1(b: 1, a: 1)
    test1()
    test1(b: 2)
    test1(a: 1)
    test1(a: 1, <!sema_unordered_arguments!>1<!>)
    test1(<!NEED_NAMED_ARGUMENT!>1<!>, a: 1)
    test1(<!NEED_NAMED_ARGUMENT!>""<!>, "")
    test1<!sema_wrong_number_of_arguments!>("", "", "")<!>
    return 0
}

func fun(a: Int32): Int32 {
    return a
}

func callFunc(myfunc: (Int32, Int32) -> Int32): Int32 {
    return myfunc<!sema_wrong_number_of_arguments!>(0)<!>
}

func foo(): Int32 {
    return callFunc(<!sema_no_match_function_declaration_for_ref!>f<!>un)
}

func bar() {
    return true;
    <!sema_incompatible_func_body_and_return_type!>1<!>
}

func baz() {
    while (true) {
        return false
    };
    <!sema_incompatible_func_body_and_return_type!>1<!>
}
