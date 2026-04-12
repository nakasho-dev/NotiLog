package org.ukky.notitrace.ui.navigation

/**
 * Navigation Compose のルート定義。
 * 型安全なルートを sealed interface で管理する。
 */
sealed interface Route {
    val route: String

    data object Onboarding : Route { override val route = "onboarding" }
    data object Home : Route { override val route = "home" }
    data object Search : Route { override val route = "search" }
    data object TagManage : Route { override val route = "tags" }
    data object Settings : Route { override val route = "settings" }
    data object OssLicenses : Route { override val route = "oss_licenses" }

    data class Detail(val id: Long) : Route {
        override val route = "detail/$id"
        companion object {
            const val ROUTE_PATTERN = "detail/{id}"
            const val ARG_ID = "id"
        }
    }

    data class JsonViewer(val id: Long) : Route {
        override val route = "detail/$id/json"
        companion object {
            const val ROUTE_PATTERN = "detail/{id}/json"
            const val ARG_ID = "id"
        }
    }
}

