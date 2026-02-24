package com.example.bustrackingapp.core.presentation.components

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ---------------------------------------------------------------------------
//  GeoPoint Evaluator — enables ValueAnimator to interpolate between coordinates
// ---------------------------------------------------------------------------
private val geoPointEvaluator = TypeEvaluator<GeoPoint> { fraction, start, end ->
    GeoPoint(
        start.latitude  + fraction * (end.latitude  - start.latitude),
        start.longitude + fraction * (end.longitude - start.longitude),
    )
}

// ---------------------------------------------------------------------------
//  Composable Map Factory
// ---------------------------------------------------------------------------
@Composable
fun rememberMapView(context: Context): MapView {
    val mapView = remember { MapView(context) }
    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }
    return mapView.apply { initMap(this) }
}

// ---------------------------------------------------------------------------
//  Map Initialisation
// ---------------------------------------------------------------------------
fun initMap(mapView: MapView) {
    mapView.apply {
        isHorizontalMapRepetitionEnabled = false
        isVerticalMapRepetitionEnabled   = false
        setMultiTouchControls(true)
        val ts = MapView.getTileSystem()
        setScrollableAreaLimitDouble(
            BoundingBox(ts.maxLatitude, ts.maxLongitude, ts.minLatitude, ts.minLongitude)
        )
        minZoomLevel = 4.0
        controller.setZoom(15.0)
    }
}

// ---------------------------------------------------------------------------
//  Map Events
// ---------------------------------------------------------------------------
fun mapEventsOverlay(
    context: Context,
    view: MapView,
    onTap: (GeoPoint) -> Unit,
): MapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
    override fun singleTapConfirmedHelper(geoPoint: GeoPoint?): Boolean {
        if (geoPoint != null) { onTap(geoPoint); view.invalidate() }
        return true
    }
    override fun longPressHelper(p: GeoPoint?) = false
})

// ---------------------------------------------------------------------------
//  Static Marker
// ---------------------------------------------------------------------------
fun addMarker(
    mapView: MapView,
    geoPoint: GeoPoint,
    title: String?    = null,
    snippet: String?  = null,
    icon: Drawable?   = null,
): Marker {
    val marker = Marker(mapView).apply {
        position = geoPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title   = title
        snippet?.let { this.snippet = it }
        icon?.let    { this.icon    = it }
    }
    mapView.overlays.add(marker)
    return marker
}

// ---------------------------------------------------------------------------
//  Animated Bus Marker
//
//  Smoothly glides the marker from its current position to [targetPoint] over
//  [durationMs] milliseconds and rotates the icon to [headingDegrees].
//  Call this every time a new tracking packet arrives instead of setting
//  marker.position directly, e.g.:
//
//    LaunchedEffect(busLocation) {
//        animateMarker(mapView, busMarker, newGeoPoint, heading = packet.headingDegree)
//    }
// ---------------------------------------------------------------------------
fun animateMarker(
    mapView: MapView,
    marker: Marker,
    targetPoint: GeoPoint,
    headingDegrees: Float = 0f,
    durationMs: Long = 3_000L,
) {
    val startPoint = GeoPoint(marker.position)

    ValueAnimator.ofObject(geoPointEvaluator, startPoint, targetPoint).apply {
        duration     = durationMs
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            val interpolated = animator.animatedValue as GeoPoint
            marker.position = interpolated
            marker.rotation = -headingDegrees   // osmdroid rotation is clockwise-negative
            mapView.invalidate()
        }
        start()
    }
}

// ---------------------------------------------------------------------------
//  Polyline
// ---------------------------------------------------------------------------
fun createPolyline(
    mapView: MapView,
    startPoint: GeoPoint,
    endPoint: GeoPoint,
): Polyline = Polyline(mapView).apply {
    color = 0xFF0000FF.toInt()
    addPoint(startPoint)
    addPoint(endPoint)
    infoWindow = null
}

// ---------------------------------------------------------------------------
//  Internal helpers
// ---------------------------------------------------------------------------
@Suppress("unused")
private fun resizeDrawable(drawable: Drawable?, newWidth: Int, newHeight: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable?.setBounds(0, 0, canvas.width, canvas.height)
    drawable?.draw(canvas)
    return bitmap
}
