// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public interface I1<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
}

public interface I2<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
    func containsValue(value: V): Bool {
        false
    }
}

extend<V> CPointer<V> <: I1<V> {}

extend<V> CPointer<V> <: I2<V> {}

main() {
    0
}

/* SCAN
error: interface function 'containsValue' must be implemented in 'extend CPointer<Generics-V>'
  ==> main_pointer.cj:24:1:
   | 
24 | extend<V> CPointer<V> <: I1<V> {}
   | ^ 
   | 

error: interface function 'containsValue' must be implemented in 'extend CPointer<Generics-V>'
  ==> main_pointer.cj:26:1:
   | 
26 | extend<V> CPointer<V> <: I2<V> {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
