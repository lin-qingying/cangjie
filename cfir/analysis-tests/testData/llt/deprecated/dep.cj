// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

func foo(<!parse_deprecated_invalid_target!>@Deprecated[<!parse_deprecated_empty_string_argument!>""<!>]<!> a!: Int64) {
    a
}

main() {
    foo(a: 10)
}
