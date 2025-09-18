package com.lablabla.feathershield.ui.auth
import android.app.Activity
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.lablabla.feathershield.R
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme

// --- STATE AND ACTIONS DEFINITIONS ---

// Represents the state of the UI, containing only what's needed for rendering.
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val authState: AuthState = AuthState.Idle
)

// Represents all possible user interactions or events from the UI.
sealed interface LoginAction {
    data class OnEmailChange(val email: String) : LoginAction
    data class OnPasswordChange(val password: String) : LoginAction
    object OnTogglePasswordVisibility : LoginAction
    object OnSignInClick : LoginAction
    object OnSignUpClick : LoginAction
    object OnGoogleSignInClick : LoginAction
    data class OnGoogleSignInResult(val idToken: String?) : LoginAction
    data class OnGoogleSignInError(val message: String) : LoginAction
    object OnErrorShown : LoginAction
}
// --- STATEFUL CONTAINER (THE ROUTE) ---

@Composable
fun LoginRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // This launcher handles the result of the Google Sign-In activity.
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.handleAction(LoginAction.OnGoogleSignInResult(account.idToken))
            } catch (e: ApiException) {
                viewModel.handleAction(LoginAction.OnGoogleSignInError(e.message ?: "Google Sign-In failed."))
            }
        } else {
            viewModel.handleAction(LoginAction.OnGoogleSignInError("Google Sign-In was canceled."))
        }
    }

    // This effect observes the authState for changes and reacts with side-effects like navigation or toasts.
    LaunchedEffect(uiState.authState) {
        when (val state = uiState.authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.handleAction(LoginAction.OnErrorShown) // Reset the state after showing the error
            }
            else -> {}
        }
    }

    // Pass state down and hoist events up.
    LoginScreen(
        state = uiState,
        onAction = { action ->
            if (action is LoginAction.OnGoogleSignInClick) {
                // Launch the sign-in intent when the google button is clicked
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } else {
                viewModel.handleAction(action)
            }
        }
    )
}


// --- STATELESS UI COMPONENT ---

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "FeatherShield Logo",
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to FeatherShield",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, "Email Icon") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password Icon") },
                trailingIcon = {
                    IconButton(onClick = { onAction(LoginAction.OnTogglePasswordVisibility) }) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (state.isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onAction(LoginAction.OnSignInClick) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign In", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onAction(LoginAction.OnSignUpClick) }) {
                    Text("Sign Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text("OR", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = { onAction(LoginAction.OnGoogleSignInClick) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google sign-in",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Text("Sign in with Google", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        AnimatedVisibility(
            visible = state.authState is AuthState.Loading,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


// --- PREVIEW SECTION ---

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
fun LoginScreenPreview_Light() {
    FeatherShieldTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                state = LoginUiState(email = "test@example.com"),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun LoginScreenPreview_Dark() {
    FeatherShieldTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                state = LoginUiState(password = "12345"),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Loading State", showBackground = true, widthDp = 360)
@Composable
fun LoginScreenPreview_Loading() {
    FeatherShieldTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                state = LoginUiState(
                    email = "loading@example.com",
                    password = "password", // pragma: allowlist secret
                    authState = AuthState.Loading
                ),
                onAction = {}
            )
        }
    }
}