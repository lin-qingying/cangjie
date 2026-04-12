// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface Graph<G, E, V> where G <: Graph<G, E, V>, E <: Edge<G, E, V>, V <: Vertex<G, E, V> {
    // func getVertices(): List<V>;
}

interface Edge<G, E, V> where G <: Graph<G, E, V>, E <: Edge<G, E, V>, V <: Vertex<G, E, V> {
    // func getGraph():G;
    // func getSource():V;
    // func getTarget():V;
}

interface Vertex<G, E, V> where G <: Graph<G, E, V>, E <: Edge<G, E, V>, V <: Vertex<G, E, V> {
    // func getGraph() :G;
    // func getIncoming() :List<E>;
    //func getOutgoing() :List<E>;
}

class Map <: Graph<Map, Road, City> {}

class Road <: Edge<Map, Road, City> {}

class City <: Vertex<Map, Road, City> {}

func f<X, Y, Z>(a: X, b: Y, c: Z) where X <: Graph<X, Y, Z>, Y <: Edge<X, Y, Z>, Z <: Vertex<X, Y, Z> {}

main() {
    f(Map(), Road(), City());
    0
}
