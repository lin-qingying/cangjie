


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

open class Map <: Graph<Map, Road, City> {}

open class Road <: Edge<Map, Road, City> {}

open class City <: Vertex<Map, Road, City> {}

class MMap <: Map {}

class RRoad <: Road {}

class CCity <: City {}

func f<X, Y, Z>(a: X, b: Y, c: Z) where X <: Graph<X, Y, Z>, Y <: Edge<X, Y, Z>, Z <: Vertex<X, Y, Z> {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f<!>(MMap(), RRoad(), CCity());
    0
}
