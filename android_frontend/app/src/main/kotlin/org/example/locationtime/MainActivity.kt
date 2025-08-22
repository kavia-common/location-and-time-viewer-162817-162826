package org.example.locationtime

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * PUBLIC_INTERFACE
 * MainActivity (View-based) inflates XML, handles permissions, observes ViewModel, and updates UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(this)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            viewModel.refresh()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleLocation = findViewById<TextView>(R.id.titleLocation)
        val textLocation = findViewById<TextView>(R.id.textLocation)
        val titleTime = findViewById<TextView>(R.id.titleTime)
        val textTime = findViewById<TextView>(R.id.textTime)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val buttonRefresh = findViewById<Button>(R.id.buttonRefresh)

        // Observe UI state
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progress.isVisible = state.loading
                textLocation.text = state.locationText
                textTime.text = state.localTimeText
                // For user-friendly errors, show as a toast
                state.message?.let {
                    android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_LONG).show()
                    viewModel.clearMessage()
                }
            }
        }

        buttonRefresh.setOnClickListener {
            if (!viewModel.hasLocationPermission()) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                viewModel.refresh()
            }
        }

        // Initial fetch
        if (viewModel.hasLocationPermission()) {
            viewModel.refresh()
        } else {
            // Prompt on first open
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
