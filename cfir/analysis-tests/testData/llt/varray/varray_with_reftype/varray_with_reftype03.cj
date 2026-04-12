// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct S1 {
    var a: (() -> Unit, Int64) = ({=>}, 123)
}

struct S2 {
    func foo(): Unit {}
}

struct S3 {
    var a: () -> Unit = {=>}
}

struct S4 {
    var a: CFunc<() -> Unit> = {=>}
}

func foo(): Unit {}

@C
func cfoo(): Unit {}

main(): Int64 {
    // Tuple
    var a1: VArray<S1, $1> = [S1()] // error
    var a2: VArray<(Array<Int64>, String), $1> = [([1, 2, 3], "123")] // error
    var a3: VArray<(Int32, Int64), $1> = [(2, 1)] // pass
    var a4: VArray<(CFunc<() -> Unit>, () -> Unit), $1> = [({=>}, {=>})] // error

    // Lambda
    var b1: VArray<S2, $1> = [S2()] // pass
    var b2: VArray<S3, $1> = [S3()] // error
    var b3: VArray<() -> Unit, $1> = [foo] // error
    var b4: VArray<CFunc<() -> Unit>, $1> = [cfoo] // pass
    var b5: VArray<S4, $1> = [S4()] // pass
    0
}

/* SCAN
error: 'Struct-S1' directly or indirectly contains an unsupported type
  ==> varray_with_reftype03.cj:32:20:
   | 
32 |     var a1: VArray<S1, $1> = [S1()] // error
   |                    ^^ contain unsupported instance member variable with type '() -> Unit'
   | 

error: 'Tuple<Struct-Array<Int64>, Struct-String>' directly or indirectly contains an unsupported type
  ==> varray_with_reftype03.cj:33:20:
   | 
33 |     var a2: VArray<(Array<Int64>, String), $1> = [([1, 2, 3], "123")] // error
   |                    ^^^^^^^^^^^^^^^^^^^^^^ contain unsupported instance member variable with type 'Struct-Array<Int64>'
   | 

error: 'Tuple<CFunc<() -> Unit>, () -> Unit>' directly or indirectly contains an unsupported type
  ==> varray_with_reftype03.cj:35:20:
   | 
35 |     var a4: VArray<(CFunc<() -> Unit>, () -> Unit), $1> = [({=>}, {=>})] // error
   |                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ contain unsupported instance member variable with type '() -> Unit'
   | 

error: 'Struct-S3' directly or indirectly contains an unsupported type
  ==> varray_with_reftype03.cj:39:20:
   | 
39 |     var b2: VArray<S3, $1> = [S3()] // error
   |                    ^^ contain unsupported instance member variable with type '() -> Unit'
   | 

error: '() -> Unit' directly or indirectly contains an unsupported type
  ==> varray_with_reftype03.cj:40:20:
   | 
40 |     var b3: VArray<() -> Unit, $1> = [foo] // error
   |                    ^^^^^^^^^^ contain unsupported instance member variable with type '() -> Unit'
   | 

5 errors generated, 5 errors printed.
*/
