// FILE: apilevel.cj


package a

@Annotation
public class APILevel {
    public let level: UInt8
    public let atomicservice: Bool
    public let stagemodeonly: Bool
    public let syscap: String

    public const init(l: UInt8, atomicservice!: Bool = false, stagemodeonly!: Bool = false, syscap!: String = "") {
        level = l
        this.stagemodeonly = stagemodeonly
        this.syscap = syscap
        this.atomicservice = atomicservice
    }
}

// FILE: key.cj



import a.*

@APILevel[18, stagemodeonly: true, syscap: "1"]
public let MAX_KEY_LENGTH = 1024

main() {}
