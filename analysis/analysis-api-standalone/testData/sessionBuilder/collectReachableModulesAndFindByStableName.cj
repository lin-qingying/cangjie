// MODULE: helper
// FILE: helper.cj
package sample.standalone.builder.helper

class Helper {
}

// MODULE: main(helper)
// MAIN_MODULE
// FILE: main.cj
package sample.standalone.builder.main

import sample.standalone.builder.helper.Helper

func consume(): Helper {
    return Helper()
}
