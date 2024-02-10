package com.example.mobile.screens

import java.util.Stack

enum class Screens {
    MONITORING,
    SETTINGS,
    EXPORT,
    PROXIMITY_SHARE,
    MAP_SCREEN,
}

class NavigationHistory(currentScreen: Screens) {
    private val backStack = Stack<Screens>()

    val currentScreen: Screens
        get() = backStack.peek()

    init {
        backStack.push(currentScreen)
    }

    fun navigateTo(screen: Screens) {
        backStack.push(screen)
    }

    fun navigateBack(): Screens {
        backStack.pop()
        return currentScreen
    }

    fun isLast(): Boolean {
        return backStack.size == 1
    }
}