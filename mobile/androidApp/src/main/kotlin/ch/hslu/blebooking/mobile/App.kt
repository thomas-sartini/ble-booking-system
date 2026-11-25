package ch.hslu.blebooking.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import ch.hslu.blebooking.mobile.ui.AppViewModel
import ch.hslu.blebooking.mobile.ui.AuthInitialState
import ch.hslu.blebooking.mobile.ui.navigation.AppNavHost
import ch.hslu.blebooking.mobile.ui.navigation.AppScreens

@Composable
fun App(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val initialState by appViewModel.initialState.collectAsStateWithLifecycle()

    when (initialState) {
        is AuthInitialState.Failed -> {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.session_restore_error))
                    Button(onClick = appViewModel::checkExistingSession) {
                        Text(stringResource(R.string.session_restore_retry))
                    }
                }
            }
        }
        is AuthInitialState.Loading -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is AuthInitialState.Authenticated -> {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = AppScreens.HOME.name
            )
        }
        is AuthInitialState.Unauthenticated -> {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = AppScreens.LOGIN.name
            )
        }
    }
}

