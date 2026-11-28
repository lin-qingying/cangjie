// MODULE: provider
// FILE: declarations.cj
package g4provider

// 语义取自 g4-source-dependency/provider/Declarations.cj 的官方 v1.1.3 探针。
public class Receiver {
    public init() {}
}

extend Receiver {
    private func privateValue(): Int64 {
        17
    }

    public func visibleValue(): Int64 {
        privateValue()
    }
}

public interface Bound {}

public class Fits <: Bound {
    public init() {}
}

public class Misses {
    public init() {}
}

public class ProbeBox<T> {
    public init() {}
}

extend<T> ProbeBox<T> where T <: Bound {
    public func boundedValue(): Int64 {
        29
    }
}

// MODULE: auxiliary
// FILE: auxiliary.cj
package g4auxiliary

public class AuxiliaryReceiver {
    public init() {}
}

extend AuxiliaryReceiver {
    public func auxiliaryValue(): Int64 {
        43
    }
}

public func auxiliaryHelper(): Int64 {
    59
}

// MODULE: consumer(provider auxiliary)
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package g4consumer

private import g4provider.{Receiver, ProbeBox, Bound, Fits}
private import g4auxiliary.{AuxiliaryReceiver, auxiliaryHelper}

public func useVisiblePrivateImport(): Int64 {
    Receiver().visibleValue()
}

public func useOtherDependency(): Int64 {
    AuxiliaryReceiver().auxiliaryValue()
}

public func useAuxiliaryHelper(): Int64 {
    auxiliaryHelper()
}

public func useBoundedExtension(): Int64 {
    ProbeBox<Fits>().boundedValue()
}

// FILE: privateMember.cj
package g4consumer

private import g4provider.Receiver

public func usePrivateMember(): Int64 {
    Receiver().privateValue()
}

// FILE: missingBoundImport.cj
package g4consumer

private import g4provider.{ProbeBox, Fits}

public func useWithoutBoundImport(): Int64 {
    ProbeBox<Fits>().boundedValue()
}

// FILE: unsatisfiedBound.cj
package g4consumer

private import g4provider.{ProbeBox, Bound, Misses}

public func useUnsatisfiedBound(): Int64 {
    ProbeBox<Misses>().boundedValue()
}

// MODULE: publicConsumer(provider)
// FILE: publicImport.cj
package g4consumer

public import g4provider.Receiver

public func useVisiblePublicImport(): Int64 {
    Receiver().visibleValue()
}
