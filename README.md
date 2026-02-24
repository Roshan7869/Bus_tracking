# Bus Tracking Application

The Bus Tracking application is a comprehensive solution designed to enhance the efficiency and convenience of bus transportation services. The app provides real-time bus tracking, real time updates on map, and timely information to users, ensuring a seamless and reliable travel experience. It leverages advanced technologies, such as Node.js, Android Kotlin and Jetpack Compose, MongoDB, and Socket.IO, to deliver a robust and user-friendly application.

BackEnd - https://github.com/deepak252/Bus-Tracking-Backend

## Features : 
**1. User Registration and Login:**
- Users register and create accounts within the app using their email and password.
- Upon successful registration, users can log in to the app using their credentials to access the full range of features.
  
**2. Real-time Bus Tracking:**
- The app integrates with GPS technology to track the location of buses in real-time.
- Bus location data is continuously updated and transmitted to the app's backend system.
  
**3. Nearby Buses and Stops:**
- The app utilizes the user's current location to identify nearby buses and stops.
- Users can access a "Nearby" feature that displays buses and stops in close proximity to their current location.
  
**4. Bus Routes and Stops:**
- The app retrieves and displays a list of available bus routes and associated stops from the backend database.
- Users can search for specific routes, view route details, and select their desired bus stops.

**5. Estimated Arrival Times:**
- The app calculates and displays estimated arrival times for buses at designated stops.
- Users can view the estimated arrival times for their selected bus stops to plan their journeys accordingly.

**6. Bus Schedule and Timetable:**
- The app retrieves the latest bus schedules and timetables from the backend system.
- Users can access the schedule information to check the departure and arrival times of buses at different stops.
  
**7. User Profile Management:**
- Users can check their profiles within the app.
  
**9. Interactive Maps:**
- The app utilizes interactive maps to display bus routes, stops, and real-time bus locations.
- Users can zoom in/out, pan the map, and interact with the interface to visualize bus information and plan their trips effectively.

<br>

<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 00 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/68c8ed30-bfe6-4388-9ccf-d17b3aed16b3">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 16 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/00c18572-a8c1-4119-8019-529f62d819c2">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 21 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/76552460-b28b-4de1-8551-f3d03a369634">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 21 PM copy" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/238a1020-00b5-4dd6-b564-42707f5050b3">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 25 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/fc304d6f-ec4a-4b9a-9da1-5718907949d9">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 31 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/48fd89ec-7867-4f3f-9cbc-6b667078cf14">
<img height="440" width="200" alt="Screenshot 2023-12-30 at 2 10 40 PM" src="https://github.com/deepak252/Bus-Tracking-App-Kotlin/assets/72331440/b93c0c96-113e-4fed-8231-e1db1c376adb">


## Driver-side CEBO Mobile Tracking Setup

This repository now includes a full driver telemetry pipeline based on **Smartphone GNSS + Cell Tower Hybrid Tracking**.

### What is implemented
- Foreground service (`BusTrackingForegroundService`) with wake lock, 3s GNSS request interval, 2s fastest interval, 5m displacement, and 15s heartbeat uplink.
- Hybrid location source flagging (`GNSS` vs `CELL_TOWER`) and low-confidence mode activation when accuracy degrades (>30m or low satellite quality).
- JSON telemetry packet model with bus/driver identifiers, coordinates, speed, heading, accuracy, source, timestamp, battery, and network type.
- Secure HTTPS transmission through Retrofit + app interceptors with retry policy (5 attempts, 10 seconds interval).
- Offline buffering up to 500 records in local persistent storage and bulk sync on reconnect.
- Auto-restart on boot using `TrackingBootReceiver` for persistent fleet operation.
- Driver control panel (`HomeDriverScreen`) to start/stop tracking with Bus ID and Driver ID.

### Android permissions and runtime behavior
- Added permissions for foreground service, wake lock, background location, and boot-completed restart.
- Tracking runs in a persistent foreground notification channel for Android background execution compliance.

### Backend expectations
Expose the following REST endpoints on the same backend base URL:
- `POST /api/tracking/location`
- `POST /api/tracking/location/bulk`

Payload format matches `TrackingPacket` under `feature_tracking/domain/model`.

## Consumer-Readiness hardening (2026 update)

Recent platform hardening aligned to consumer app standards:
- Role-aware home experience: drivers now land on dedicated tracking console while passengers continue to home feed.
- Startup routing stabilized with state-based initial route flow (splash/auth/dashboard).
- Security posture improved by moving API/socket hosts to `BuildConfig` and removing legacy storage flags.
- Permission UX now supports a non-blocking limited mode instead of hard lock dialogs.
- Tracking lifecycle improved with no-location watchdog and low-battery alert de-duplication.
