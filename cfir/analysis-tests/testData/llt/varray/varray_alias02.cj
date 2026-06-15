// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type varr2 = VArray<!parse_varray_type_parameter!><!>
main() {
  var c: varr2<Int64, <!parse_expected_right_delimiter, parse_expected_type!>$<!>3> = varr2<Int64, $3>(repeat: 0)
}
