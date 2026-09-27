package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.example.data.remote.NetworkModule
import com.example.data.repository.AuthoritativeContentProvider
import com.example.domain.model.UserLocation
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

@Composable
fun CityPickerDialog(
    currentLocation: UserLocation,
    primaryLocation: UserLocation? = null,
    onLocationSelected: (UserLocation) -> Unit,
    onSetAsPrimaryLocation: (UserLocation) -> Unit = {},
    onClearPrimaryLocation: () -> Unit = {},
    onUseCurrentLocation: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchingLive by remember { mutableStateOf(false) }
    var liveSearchResults by remember { mutableStateOf<List<UserLocation>>(emptyList()) }

    val isCurrentPrimary = primaryLocation != null && 
        primaryLocation.cityName.equals(currentLocation.cityName, ignoreCase = true)

    // Live geocoding search across any city in the world
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length < 2) {
            isSearchingLive = false
            liveSearchResults = emptyList()
            return@LaunchedEffect
        }

        isSearchingLive = true
        delay(350L) // Debounce typing

        try {
            val results = withContext(Dispatchers.IO) {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=20&language=en&format=json"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Baqiyyah-App/1.0")
                    .build()

                val response = NetworkModule.okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val root = JSONObject(bodyString)
                        val resultsArray = root.optJSONArray("results")
                        if (resultsArray != null && resultsArray.length() > 0) {
                            val list = mutableListOf<UserLocation>()
                            for (i in 0 until resultsArray.length()) {
                                val item = resultsArray.getJSONObject(i)
                                val name = item.getString("name")
                                val country = item.optString("country", "")
                                val admin1 = item.optString("admin1", "")
                                val lat = item.getDouble("latitude")
                                val lon = item.getDouble("longitude")
                                val displayRegion = when {
                                    admin1.isNotBlank() && country.isNotBlank() && admin1 != name -> "$admin1, $country"
                                    country.isNotBlank() -> country
                                    else -> admin1
                                }
                                list.add(
                                    UserLocation(
                                        cityName = name,
                                        countryName = displayRegion,
                                        latitude = lat,
                                        longitude = lon,
                                        isAutoDetected = false
                                    )
                                )
                            }
                            return@withContext list
                        }
                    }
                }
                emptyList()
            }

            if (results.isNotEmpty()) {
                liveSearchResults = results
            } else {
                // Fallback to local matching if API had no results
                liveSearchResults = AuthoritativeContentProvider.cities.filter {
                    it.cityName.contains(query, ignoreCase = true) ||
                    it.countryName.contains(query, ignoreCase = true)
                }
            }
        } catch (_: Exception) {
            // Fallback to local matching on network error
            liveSearchResults = AuthoritativeContentProvider.cities.filter {
                it.cityName.contains(query, ignoreCase = true) ||
                it.countryName.contains(query, ignoreCase = true)
            }
        } finally {
            isSearchingLive = false
        }
    }

    val displayList = remember(searchQuery, liveSearchResults) {
        if (searchQuery.trim().length < 2) {
            AuthoritativeContentProvider.cities
        } else {
            liveSearchResults
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .testTag("city_picker_dialog"),
            shape = AlDeenTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AlDeenTokens.SpacingLarge)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            AlDeenText(
                                text = "Select City (شہر منتخب کریں)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Live worldwide search for any city",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Location Card / Toggle Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrentPrimary) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isCurrentPrimary) EmeraldPrimary else MintBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("primary_location_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isCurrentPrimary) GoldAccent else EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    AlDeenText(
                                        text = if (isCurrentPrimary) {
                                            "★ ${currentLocation.cityName} is Primary Location"
                                        } else {
                                            "Set ${currentLocation.cityName} as your Primary Location"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentPrimary) EmeraldPrimary else TextPrimary,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    AlDeenText(
                                        text = if (isCurrentPrimary) {
                                            "${currentLocation.cityName} بنیادی مقام ہے (نوٹیفکیشن و ایپ اوپن پر رہے گا)"
                                        } else {
                                            "${currentLocation.cityName} کو اپنا مستقل / بنیادی مقام بنائیں"
                                        },
                                        style = AlDeenTypography.UrduBody.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = isCurrentPrimary,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onSetAsPrimaryLocation(currentLocation)
                                        Toast.makeText(
                                            context,
                                            "${currentLocation.cityName} set as Primary Location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onClearPrimaryLocation()
                                        Toast.makeText(
                                            context,
                                            "Primary location cleared",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = MintBorder
                                ),
                                modifier = Modifier.testTag("primary_location_switch")
                            )
                        }

                        if (!isCurrentPrimary && primaryLocation != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.7.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlDeenText(
                                    text = "Current Primary: ${primaryLocation.cityName} (بنیادی مقام)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                                TextButton(
                                    onClick = {
                                        onLocationSelected(primaryLocation)
                                        onDismissRequest()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    AlDeenText(
                                        text = "Switch to ${primaryLocation.cityName}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // "Use Current Location" Action Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldContainer,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUseCurrentLocation()
                            onDismissRequest()
                        }
                        .testTag("use_current_location_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use Current Location",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Current Location",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 14.sp
                                )
                            )
                            AlDeenText(
                                text = "موجودہ لوکیشن استعمال کریں (Auto-detect via GPS)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                        if (currentLocation.isAutoDetected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { AlDeenText("Search city e.g. Faisalabad, London, Madinah ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (isSearchingLive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = AlDeenTokens.ShapePill,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MintBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section Label
                Text(
                    text = if (searchQuery.trim().length >= 2) "Worldwide Search Results" else "Popular Cities",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 11.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // List of Cities
                if (displayList.isEmpty() && !isSearchingLive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cities found matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(displayList) { city ->
                            val isSelected = city.cityName.equals(currentLocation.cityName, ignoreCase = true)
                            val isCityPrimary = primaryLocation != null && primaryLocation.cityName.equals(city.cityName, ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLocationSelected(city)
                                        onDismissRequest()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) EmeraldPrimary else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = city.cityName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) EmeraldPrimary else TextPrimary,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            if (isCityPrimary) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = GoldLight
                                                ) {
                                                    Text(
                                                        text = "★ Primary",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = GoldAccent,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = city.countryName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isCityPrimary) {
                                                onClearPrimaryLocation()
                                                Toast.makeText(context, "Cleared ${city.cityName} from Primary", Toast.LENGTH_SHORT).show()
                                            } else {
                                                onSetAsPrimaryLocation(city)
                                                Toast.makeText(context, "${city.cityName} set as Primary Location", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCityPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Toggle Primary Location",
                                            tint = if (isCityPrimary) GoldAccent else TextTertiary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.7.dp)
                        }
                    }
                }
            }
        }
    }
}
