// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type C<T> = CPointer<T>

var node4_2: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>C<!> = C<Int64>() // error.

type D = C<Int64>

var node5_2: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>C<!> = D() // error.

foreign func malloc(size: UIntNative): CPointer<Unit>

var node17 = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer(malloc)<!> // error.
var node8: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>CPointer<!> = CPointer<Unit>() // error.
var node9: CType = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer()<!> // error.
var node14: CString = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer()<!> // error.
var node15 = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer()<!> // error.
var node16: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>CPointer<!> = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer()<!> // error.

var node13 = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer(CPointer<Unit>())<!> // error.

main() {}
