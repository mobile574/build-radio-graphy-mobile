package com.squareup.radiography.sample.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog.Builder
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import radiography.Radiography
import radiography.ViewFilter
import radiography.ViewFilters.FocusedWindowViewFilter
import radiography.ViewFilters.and
import radiography.ViewStateRenderers.CheckableRenderer
import radiography.ViewStateRenderers.DefaultsIncludingPii
import radiography.ViewStateRenderers.DefaultsNoPii
import radiography.ViewStateRenderers.ViewRenderer
import radiography.ViewStateRenderers.textViewRenderer
import radiography.ViewStateRenderers.viewStateRendererFor

@Composable fun ComposeSampleApp() {
  val (isChecked, onCheckChanged) = remember { mutableStateOf(false) }
  var text by remember { mutableStateOf("") }
  val context = ContextAmbient.current

  Column {
    Text("The password is Baguette", style = MaterialTheme.typography.body2)
    Row(verticalGravity = Alignment.CenterVertically) {
      Checkbox(checked = isChecked, onCheckedChange = onCheckChanged)
      Text("Check me, or don't.")
    }
    TextField(value = text, onValueChange = { text = it }, label = { Text("Text Field") })
    // Include a classic Android view in the composition.
    AndroidView(::TextView) {
      @SuppressLint("SetTextI18n")
      it.text = "inception"
    }
    Box(Modifier.testTag("show-rendering")) {
      Button(onClick = { showSelectionDialog(context) }) {
        Text("Show string rendering dialog")
      }
    }
  }
}

private fun showSelectionDialog(context: Context) {
  val renderings = listOf(
      "Default" to {
        Radiography.scan()
      },
      "Focused window" to {
        Radiography.scan(viewFilter = FocusedWindowViewFilter)
      },
      "Focused window and custom filter" to {
        Radiography.scan(viewFilter = FocusedWindowViewFilter and object : ViewFilter {
          override fun matches(view: Any): Boolean = view !is LinearLayout
        })
      },
      "Include PII" to {
        Radiography.scan(viewStateRenderers = DefaultsIncludingPii)
      },
      "Include PII ellipsized" to {
        Radiography.scan(
            viewStateRenderers = listOf(
                ViewRenderer,
                textViewRenderer(includeTextViewText = true, textViewTextMaxLength = 4),
                CheckableRenderer
            )
        )
      },
      "Custom LinearLayout renderer" to {
        Radiography.scan(
            viewStateRenderers = DefaultsNoPii + viewStateRendererFor<LinearLayout> {
              append(if (it.orientation == LinearLayout.HORIZONTAL) "horizontal" else "vertical")
            })
      }
  )

  val items = renderings.map { it.first }
      .toTypedArray()
  Builder(context)
      .setTitle("Choose rendering")
      .setItems(items) { _, index ->
        val rendering = renderings[index].second()
        // Print each line as a separate logcat entry so the total output doesn't get truncated.
        rendering.lineSequence().forEach {
          Log.d("MainActivity", it)
        }
        showResult(context, rendering)
      }
      .show()
}

private fun showResult(
  context: Context,
  rendering: String
) {
  val renderingDialog = Builder(context)
      .setTitle("Rendering (also printed to Logcat)")
      .setMessage(rendering)
      .setPositiveButton("Ok") { _, _ ->
        showSelectionDialog(context)
      }
      .show()
  val messageView = renderingDialog.findViewById<TextView>(android.R.id.message)!!
  messageView.textSize = 9f
  messageView.typeface = Typeface.MONOSPACE
}
