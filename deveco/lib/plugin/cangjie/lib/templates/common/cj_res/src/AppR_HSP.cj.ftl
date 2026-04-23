package cj_res_${moduleName}

public class AppString {
	public var app_name = 33554432
	public var shared_desc = 33554434
}

public class AppColor {
	public var white = 33554435
}

public class AppMedia {
	public var app_icon = 33554433
	public var icon = 33554436
	public var setting = 33554437
}

public class AppProfile {
	public var main_pages = 33554438
}

public class app {
	public static let string = AppString()
	public static let color = AppColor()
	public static let media = AppMedia()
	public static let profile = AppProfile()
}
