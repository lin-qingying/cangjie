// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {}

extend CPointer<Int64> <: <!EXTEND_DUPLICATE_INTERFACE!>I<Int64><!> {} // error

extend<T> CPointer<T> <: I<T> {} // error

main() {}
