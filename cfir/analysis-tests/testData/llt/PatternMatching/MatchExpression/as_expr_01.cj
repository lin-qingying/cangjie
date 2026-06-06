// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct Foo {}

main() {
    let any: Any = Foo()
    let x = any as Foo <!parse_chained_none_associative!>as<!> Option<Foo>
    match (x) {
        case Some(Some(_: Foo)) => 0
        case _ => 1
    }
}
