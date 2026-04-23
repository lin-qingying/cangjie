// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class X {}

class Y {}

class A<T, K> where T <: X, K <: Y {}

extend<<!CONFLICTING_UPPER_BOUNDS!>T<!>> A<T, T> {}

main() {
}
