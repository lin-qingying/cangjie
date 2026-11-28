// RUN_PIPELINE_TILL: FRONTEND
// FILE: socket.cj
package std.net

public struct CfirRawQualifiedSockAddr {}

// FILE: typeUsages.cj
package raw.qualified

public struct Wrapper<T> {}

public interface SockAddrTypeUses {
    func returnSockAddr(): <!UNDECLARED_TYPE_NAME!>std<!>.net.CfirRawQualifiedSockAddr
    func acceptSockAddr(value: <!UNDECLARED_TYPE_NAME!>std<!>.net.CfirRawQualifiedSockAddr): Unit
    func nestedSockAddr(): Wrapper<<!UNDECLARED_TYPE_NAME!>std<!>.net.CfirRawQualifiedSockAddr>
}
