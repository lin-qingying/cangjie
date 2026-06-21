// FILE: level_syscap_check/device_info.cj


package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {19}
    }
}

// FILE: err_desugar.cj
import ohos.device_info.DeviceInfo


public func foo() {1}
public func goo() {"1"}

main() {
    <!TYPE_MISMATCH!>@IfAvailable(level:19, { => foo() }, { => goo() })<!>
}
