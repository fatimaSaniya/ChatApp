package com.example.chatapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatapp.R

@Composable
fun SignInScreenUI(
    onSignInClick: () -> Unit
) {
    val brush = Brush.linearGradient(
        listOf(
            Color.White,
            Color.Gray
        )
    )
    Image(
        painter = painterResource(id = R.drawable.back),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = null,
            modifier = Modifier.size(450.dp).padding(25.dp)
        )
        Text(
            text = "Universe of Conversations",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 30.sp
            )
        )
        Text(
            text = "Chat, laugh and connect with new friends. Dive into a fun world where conversations flow and friendships grow!",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.padding(15.dp)
        )
        Spacer(modifier = Modifier.height(70.dp))
        Button(
            onClick = {onSignInClick.invoke()},
            modifier = Modifier
                .background(brush, CircleShape)
                .fillMaxWidth(.9f)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(Color.Transparent), shape = CircleShape
        ) {
            Text(
                text = "Launch your journey with Google",
                modifier = Modifier.padding(end = 4.dp),
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Image(
                painter = painterResource(id = R.drawable.goog_0ed88f7c),
                contentDescription = null,
                modifier = Modifier.scale(.6f)
            )
        }
    }
}