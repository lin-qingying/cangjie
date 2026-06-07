// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// test type recursive.
<!sema_value_type_recursive!>s<!>truct S1 {
    var a: S2 = S2()
}

struct S2 {
    var a: VArray<S1, $1> = [<!RECURSIVE_CONSTRUCTOR_CALL!>S1<!>()]
}

main() {
    var a: S2 = S2()
    0
}
