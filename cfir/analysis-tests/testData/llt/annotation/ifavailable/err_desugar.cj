// DEPENDENCE: level_syscap_check/device_info.cj

import ohos.device_info.DeviceInfo


public func foo() {1}
public func goo() {"1"}

main() {
    <!TYPE_MISMATCH!>@IfAvailable(level:19, { => foo() }, { => goo() })<!>
}
