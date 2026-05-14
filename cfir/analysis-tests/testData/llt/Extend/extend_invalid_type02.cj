// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %overflowOpt %cmp_opt %f -o %output 

public interface <!INTERFACE_CANNOT_INHERIT_CLASS!>Strbase<!> <: Collection<Byte> & Equatable<String> & Comparable<String> & Hashable & <!UNRESOLVED_REFERENCE!>T<!> & String {}

extend String <: Strbase {}

main(): Int64 {
    0
}
