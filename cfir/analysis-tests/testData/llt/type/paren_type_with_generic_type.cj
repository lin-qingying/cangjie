// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func a() {
    let a: (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Array<!>)
    let b: (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Range<!>)
    let c: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Array<!>
}

main() {}
