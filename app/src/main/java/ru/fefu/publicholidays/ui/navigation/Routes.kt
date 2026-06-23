package ru.fefu.publicholidays.ui.navigation

import android.net.Uri

object Routes {
    const val LIST = "list"
    const val FAVOURITES = "favourites"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val ARG_ID = "holidayId"
    const val DETAIL_ROUTE = "detail/{$ARG_ID}"

    fun detailRoute(holidayId: String): String {
        return "detail/${Uri.encode(holidayId)}"
    }
}