// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {}

interface Foo {
    prop v1: Int64
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend A <: Foo {}<!>

main() {
    0
}
