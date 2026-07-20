package com.azurlize.team.utils

import org.drinkless.tdlib.TdApi

fun getUserStatus(status: TdApi.UserStatus?): String {
    return when (status) {
        is TdApi.UserStatusOnline -> "online"
        is TdApi.UserStatusOffline -> {
            if (status.wasOnline == 0) "offline"
            else "last seen recently" // simplified
        }
        is TdApi.UserStatusRecently -> "last seen recently"
        is TdApi.UserStatusLastWeek -> "last seen last week"
        is TdApi.UserStatusLastMonth -> "last seen last month"
        else -> ""
    }
}
