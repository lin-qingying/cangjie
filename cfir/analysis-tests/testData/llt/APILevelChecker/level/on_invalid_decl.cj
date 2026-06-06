// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle --cfg=APILevel=11
// ERRCHECK: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix

import <!package_search_error!>ohos.labels<!>.*

func foo(@!APILevel[since: "10", syscap: "xxx"] a: Int64) {}

@!APILevel[since: "10", syscap: "xxx"]
main() {}
