package ir.ghestyar.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.presentation.addloan.AddLoanScreen
import ir.ghestyar.app.presentation.home.HomeScreen
import ir.ghestyar.app.presentation.loandetail.LoanDetailScreen
import ir.ghestyar.app.presentation.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val ADD_LOAN = "addLoan"
    const val LOAN_DETAIL = "loanDetail/{loanId}"
    const val SETTINGS = "settings"
    fun loanDetail(id: Long) = "loanDetail/$id"
}

/** انیمیشن ورود/خروج نرم و کوتاه بین صفحات (بند ۱۰ سند طراحی) */
private val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(220)) { it / 4 } + fadeIn(tween(220))
}
private val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(180))
}
private val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(220))
}
private val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(180)) { it / 4 } + fadeOut(tween(180))
}

@Composable
fun GhestYarNavGraph(app: GhestYarApplication) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                app = app,
                onAddLoan = { navController.navigate(Routes.ADD_LOAN) },
                onOpenLoan = { id -> navController.navigate(Routes.loanDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ADD_LOAN) {
            AddLoanScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            Routes.LOAN_DETAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
            LoanDetailScreen(app = app, loanId = loanId, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(app = app, onBack = { navController.popBackStack() })
        }
    }
}
