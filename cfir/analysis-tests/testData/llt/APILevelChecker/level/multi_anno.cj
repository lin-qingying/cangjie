// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix --cfg APILevel_level=9

import ohos.labels.*

@!APILevel[since: "9"]
@!APILevel[since: "10"]
public func foo() {}

@!APILevel[since: "10"]
@!APILevel[since: "9"]
public func goo() {}

main() {
    foo()
    goo()
    0
}
