package org.ukky.notitrace.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Route 定義の単体テスト。
 *
 * 各 Route のルート文字列が仕様どおりであることを保証する。
 */
class RouteTest {

    @Test
    fun onboarding_route_is_correct() {
        assertEquals("onboarding", Route.Onboarding.route)
    }

    @Test
    fun home_route_is_correct() {
        assertEquals("home", Route.Home.route)
    }

    @Test
    fun search_route_is_correct() {
        assertEquals("search", Route.Search.route)
    }

    @Test
    fun tagManage_route_is_correct() {
        assertEquals("tags", Route.TagManage.route)
    }

    @Test
    fun settings_route_is_correct() {
        assertEquals("settings", Route.Settings.route)
    }

    @Test
    fun ossLicenses_route_is_correct() {
        assertEquals("oss_licenses", Route.OssLicenses.route)
    }

    @Test
    fun detail_route_is_correct() {
        assertEquals("detail/42", Route.Detail(42L).route)
    }

    @Test
    fun jsonViewer_route_is_correct() {
        assertEquals("detail/7/json", Route.JsonViewer(7L).route)
    }
}

