package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.DarkBg
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.InputBg
import si.uni_lj.fe.tnuv.memorymapp.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo Section (Same as AccountScreen)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "MEMORY", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 36.sp, letterSpacing = (-1).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "MAPP", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 36.sp, letterSpacing = (-1).sp)
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp).padding(start = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Welcome back", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "Log in to your account", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        AccountTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            leadingIcon = Icons.Default.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        AccountTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible }
        )

        if (error != null) {
            Text(text = error!!, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(28.dp))
                .clickable(enabled = !isLoading) { viewModel.login(email, password) },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text(text = "Log in", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text(text = "Don't have an account? ", color = Color.White)
            Text(text = "Sign up", color = Color(0xFF6E6EF7), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSignUpClick() })
        }
    }
}
