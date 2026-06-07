// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<T> {}

interface I<T>{}

extend<T> A<T> <: I<T>{
  static public func me<<!parse_expected_name!>class<!> A<T> {}

<!parse_unexpected_declaration_in_scope!>extend<!><T> A<T> <: I<T>{
  static public func me<T>() {
  }
}

<!parse_unexpected_declaration_in_scope!>main<!>(): Int64 {}<!parse_expected_right_delimiter!>
<!>
