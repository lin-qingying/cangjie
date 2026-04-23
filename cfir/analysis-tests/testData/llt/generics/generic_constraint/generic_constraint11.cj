// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

class B<K, L> where K <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>(<!GENERIC_ARGUMENT_NO_MATCH!>B<L, L><!>) -> Unit<!> {}

main() {}
