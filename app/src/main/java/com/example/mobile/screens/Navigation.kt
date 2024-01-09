package com.example.mobile.screens

import java.util.Stack

enum class Screens {
    MONITORING,
    SETTINGS,
    EXPORT
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
}