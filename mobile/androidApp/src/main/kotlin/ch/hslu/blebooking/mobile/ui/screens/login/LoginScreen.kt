package ch.hslu.blebooking.mobile.ui.screens.login

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.navigation.AppScreens
import ch.hslu.blebooking.mobile.ui.components.MessageData
import ch.hslu.blebooking.mobile.ui.components.Message
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.components.MessageType
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    messageData: MessageData? = null,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val successText = stringResource(R.string.home_login_success)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LoginContent(
        modifier = modifier,
        isLoading = loginViewModel.isLoading,
        messageData = messageData,
        errorMessage = errorMessage,
        onSignIn = { email, password ->
            errorMessage = null
            loginViewModel.signIn(
                email = email,
                password = password,
                onSuccess = {
                    navController.navigate("${AppScreens.HOME.name}?message=${Uri.encode(successText)}&type=${MessageType.Success.name}") {
                        popUpTo("${AppScreens.LOGIN.name}?message={message}&type={type}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onError = { message -> errorMessage = message },
            )
        },
        onForgotPassword = { },
    )
}

@Composable
private fun LoginContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    messageData: MessageData? = null,
    onSignIn: (String, String) -> Unit = { _, _ -> },
    onForgotPassword: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp)
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AppTopBar()
            if (errorMessage == null) messageData?.let {
                Message(text = it.text, type = it.type)
            }
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.login_title), fontSize = 36.sp, lineHeight = 43.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.login_email))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.login_email_hint)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.login_password))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.login_password_hint)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { visible = !visible }) {
                    Text(stringResource(if (visible) R.string.login_hide else R.string.login_show))
                } }, shape = MaterialTheme.shapes.medium,
            )
            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.login_forgot))
            }
            Spacer(Modifier.height(8.dp))
            errorMessage?.let { message ->
                Message(text = message, type = MessageType.Error)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    onSignIn(email.trim(), password)
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp), shape = MaterialTheme.shapes.medium) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.login_sign_in), fontSize = 18.sp)
            }
        }
    }
}

@Preview(name = "Login Light", showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    BleTheme(darkTheme = false) {
        LoginContent()
    }
}

@Preview(name = "Login Dark", showBackground = true)
@Composable
private fun LoginScreenDarkPreview() {
    BleTheme(darkTheme = true) {
        LoginContent()
    }
}




