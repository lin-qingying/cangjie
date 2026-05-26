// MODULE: helper
// FILE: helper.cj
package sample.standalone.packages.helper

class Helper {
}

// MODULE: main(helper)
// MAIN_MODULE
// FILE: main.cj
package sample.standalone.packages.app

import sample.standalone.packages.helper.Helper

func consume(): Helper {
    return Helper()
}
