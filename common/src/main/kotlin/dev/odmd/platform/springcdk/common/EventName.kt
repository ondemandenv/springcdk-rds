package dev.odmd.platform.springcdk.common

enum class EventName(val eventName: String) {
    PROFILE_ACTIVATED("Profile Activated"),
    PROFILE_UPDATED("Profile Updated"),
    PROFILE_DEACTIVATED("Profile Deactivated")
}