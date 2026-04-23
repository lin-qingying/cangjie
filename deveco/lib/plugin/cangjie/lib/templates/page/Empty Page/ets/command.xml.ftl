<instantiate src="ets/src/ets/page/page11.ets.ftl" dest="${rootOut}/src/main/ets/pages/${pkgDir}/${pageFileName}.ets"/>
<#--  res  -->
<#if moduleType = "entry" || moduleType = "feature">
    <merge src="ets/res/profile/main_pages.json.ftl" dest="${rootOut}/src/main/resources/base/profile/${pageProfileFileName}"/>
</#if>
<open file="${rootOut}/src/main/ets/pages/${pkgDir}/${pageFileName}.ets"/>
