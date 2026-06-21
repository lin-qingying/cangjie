

package pkga

public class A<X, Y> {}

public type BB<BX, BY> = A<BX, BY>

public type AA<XX> = BB<XX, Array<XX>>