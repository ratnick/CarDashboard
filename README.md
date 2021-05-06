# CarDashboard
General use: A dashboard built for car telemetry. 
UI targeted at trackday; i.e. clear no-nonsense visuals.
Content: tyre pressure, tyre temperature, outdoor temperature.
Built scalable to include more metrics.

Content:
Android app with interface to several sensors:
- tire temperature sensors mounted on top of tyre.
      + communicating via wifi/UDP.
      + Arduino based, battery powered, measuring with infrared heat sensor. 
- Tire pressure sensors:
      + communicating via BLE. 
      + VC601 type which seem to work best (https://www.aliexpress.com/item/32902607772.html?spm=a2g0s.9042311.0.0.27424c4dPwDtFs)
      + Chinese No Name which turned out to be unstable. But communication and decoding works (https://www.aliexpress.com/item/33041131063.html?spm=a2g0s.9042311.0.0.27424c4doWNTAi

