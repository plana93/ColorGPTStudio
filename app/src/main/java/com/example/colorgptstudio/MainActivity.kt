package com.example.colorgptstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.colorgptstudio.ui.analysis.AnalysisScreen
import com.example.colorgptstudio.ui.home.HomeScreen
import com.example.colorgptstudio.ui.project.ProjectDetailScreen
import com.example.colorgptstudio.ui.project.ProjectListScreen
import com.example.colorgptstudio.ui.quickanalysis.QuickAnalysisScreen
import com.example.colorgptstudio.ui.theme.ColorGPTStudioTheme

// ─── Route di navigazione ─────────────────────────────────────────────────────
object Routes {
    const val HOME = "home"
    const val QUICK_ANALYSIS = "quick_analysis"
    const val PROJECT_LIST = "project_list"
    const val PROJECT_DETAIL = "project/{projectId}"
    const val ANALYSIS = "analysis/{projectId}/{imageId}"

    fun projectDetail(projectId: Long) = "project/$projectId"
    fun analysis(projectId: Long, imageId: Long) = "analysis/$projectId/$imageId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorGPTStudioTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onQuickAnalysisClick = { navController.navigate(Routes.QUICK_ANALYSIS) },
                onProjectsClick = { navController.navigate(Routes.PROJECT_LIST) }
            )
        }
        composable(Routes.QUICK_ANALYSIS) {
            QuickAnalysisScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROJECT_LIST) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Routes.projectDetail(projectId))
                }
            )
        }

        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onImageClick = { pId, imageId ->
                    navController.navigate(Routes.analysis(pId, imageId))
                }
            )
        }

        composable(
            route = Routes.ANALYSIS,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("imageId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val imageId = backStackEntry.arguments?.getLong("imageId") ?: return@composable
            AnalysisScreen(
                projectId = projectId,
                imageId = imageId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
