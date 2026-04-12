// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %f apilevel.%middle

import ohos.labels.*

@!APILevel[since: "11"]
public class A {
    // @!APILevel[since: "11"]
    public func foo() {
        goo() // ok, scope level from A.
    }

    @!APILevel[since: "11"]
    public func goo() {}
}

main() {
}
