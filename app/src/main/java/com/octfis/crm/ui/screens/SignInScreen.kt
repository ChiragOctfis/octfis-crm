// app/src/main/java/com/octfis/crm/ui/screens/SignInScreen.kt
package com.octfis.crm.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.octfis.crm.R
import com.octfis.crm.data.remote.ZohoServiceLocator
import com.octfis.crm.navigation.Screen
import com.octfis.crm.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(navController: NavController) {
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()

    // Restore existing session
    LaunchedEffect(Unit) {
        val restored = ZohoServiceLocator
            .getCatalystAuthManager()
            .restoreSession()
        if (restored) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter            = painterResource(id = R.drawable.applogo),
            contentDescription = "OCTFIS TECHNO LLP",
        )

        Spacer(Modifier.height(36.dp))

        Text(
            "Sign In",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = "Enter your email and password to access the CRM",
            fontSize  = 13.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value           = email,
            onValueChange   = { email = it; errorMsg = "" },
            label           = { Text("Email") },
            placeholder     = { Text("email@company.com") },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape           = RoundedCornerShape(8.dp),
            modifier        = Modifier.fillMaxWidth(),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = CrmPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value                = password,
            onValueChange        = { password = it; errorMsg = "" },
            label                = { Text("Password") },
            placeholder          = { Text("Enter your password") },
            singleLine           = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape                = RoundedCornerShape(8.dp),
            modifier             = Modifier.fillMaxWidth(),
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = CrmPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )

        // Error message
        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text     = errorMsg,
                color    = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (email.isBlank()) { errorMsg = "Email is required"; return@Button }
                if (password.isBlank()) { errorMsg = "Password is required"; return@Button }
                isLoading = true
                errorMsg  = ""
                kotlinx.coroutines.MainScope().launch {
                    ZohoServiceLocator
                        .getCatalystAuthManager()
                        .login(email.trim(), password)
                        .fold(
                            onSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.SignIn.route) { inclusive = true }
                                }
                            },
                            onFailure = {
                                errorMsg  = it.message ?: "Login failed"
                                isLoading = false
                            }
                        )
                }
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = CrmPrimary),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.surface,
                )
            } else {
                Text(
                    "Sign In",
                    color      = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text      = "Contact your administrator to get access",
            fontSize  = 11.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))



        val annotatedString = buildAnnotatedString {

            append("By clicking continue, you agree to our ")



            withLink(

                LinkAnnotation.Url(

                    url = "https://www.octfis.com/privacy-policy",

                    styles = TextLinkStyles(

                        style = SpanStyle(

                            textDecoration = TextDecoration.Underline,

                            color = CrmPrimary

                        )

                    )

                )

            ) {

                append("Terms of Service")

            }



            append(" and ")



            withLink(

                LinkAnnotation.Url(

                    url = "https://www.octfis.com/privacy-policy",

                    styles = TextLinkStyles(

                        style = SpanStyle(

                            textDecoration = TextDecoration.Underline,

                            color = CrmPrimary

                        )

                    )

                )

            ) {

                append("Privacy Policy")

            }

        }



        Text(

            text = annotatedString,

            fontSize = 11.sp,

            color = MaterialTheme.colorScheme.onSurfaceVariant,

            textAlign = TextAlign.Center,

            )
    }
}