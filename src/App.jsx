import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import "./styles.css";

export default function App() {
  const mapRef = useRef(null);
  const markersRef = useRef({});
  const [drivers, setDrivers] = useState([]);

  const API =
    "https://script.google.com/macros/s/AKfycbw-3wKRuKImCvvB4ip3PGokDP18yJz6HDW2QylDmvQGxAbyn8Wq-FIlHQ9ms-i7wlCEQA/exec?action=drivers";

  // 🔥 تحميل الخريطة + البيانات
  useEffect(() => {
    const map = L.map("map", {
      zoomControl: false,
    }).setView([33.3152, 44.3661], 7);

    L.tileLayer(
      "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    ).addTo(map);

    mapRef.current = map;

    const loadDrivers = async () => {
      try {
        const res = await fetch(API);
        const data = await res.json();

        setDrivers(data.drivers);

        data.drivers.forEach((d) => {
          const id = d.driver;
          const latlng = [parseFloat(d.lat), parseFloat(d.lng)];

          if (markersRef.current[id]) {
            markersRef.current[id].setLatLng(latlng);
          } else {
            const color =
              d.status === "moving"
                ? "#00ff88"
                : d.status === "stopped"
                ? "#ffc107"
                : "#888";

            const marker = L.circleMarker(latlng, {
              radius: 8,
              color,
              fillColor: color,
              fillOpacity: 1,
            }).addTo(map);

            markersRef.current[id] = marker;
          }
        });
      } catch (err) {
        console.error(err);
      }
    };

    loadDrivers();
    const interval = setInterval(loadDrivers, 5000);

    return () => clearInterval(interval);
  }, []);

  // 🔥 الحسابات
  const active = drivers.filter((d) => d.status === "moving").length;
  const stopped = drivers.filter((d) => d.status === "stopped").length;
  const offline = drivers.filter((d) => d.status === "offline").length;
  const total = drivers.length || 1;

  const activePercent = Math.round((active / total) * 100);
  const stoppedPercent = Math.round((stopped / total) * 100);
  const offlinePercent = Math.round((offline / total) * 100);

  // 🔥 Animation للأرقام
  const useCounter = (value) => {
    const [count, setCount] = useState(0);

    useEffect(() => {
      let start = 0;
      const step = Math.ceil(value / 20);

      const interval = setInterval(() => {
        start += step;
        if (start >= value) {
          start = value;
          clearInterval(interval);
        }
        setCount(start);
      }, 30);

      return () => clearInterval(interval);
    }, [value]);

    return count;
  };

  const activeAnim = useCounter(active);
  const stoppedAnim = useCounter(stopped);
  const offlineAnim = useCounter(offline);

  return (
    <div className="layout">
      <Sidebar />

      <div className="main">
        <Topbar />

        {/* 🔥 الكروت */}
        <div className="cards">
          <div className="card">
            <h2>{activeAnim}</h2>
            <p>VEHICLES IN SERVICE</p>
            <div className="progress">
              <div
                style={{
                  width: activePercent + "%",
                  background: "#00ff88",
                  height: "100%",
                }}
              />
            </div>
          </div>

          <div className="card">
            <h2>{stoppedAnim}</h2>
            <p>VEHICLES NOT IN SERVICE</p>
            <div className="progress">
              <div
                style={{
                  width: stoppedPercent + "%",
                  background: "#ff3b3b",
                  height: "100%",
                }}
              />
            </div>
          </div>

          <div className="card">
            <h2>{offlineAnim}</h2>
            <p>OFFLINE</p>
            <div className="progress">
              <div
                style={{
                  width: offlinePercent + "%",
                  background: "#888",
                  height: "100%",
                }}
              />
            </div>
          </div>

          <div className="card">
            <h2>28 min</h2>
            <p>AVG TIME PER ROUTE</p>
            <div className="progress">
              <div
                style={{
                  width: "60%",
                  background: "#00c2ff",
                  height: "100%",
                }}
              />
            </div>
          </div>

          <div
            className="card"
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <div>
              <p>AVERAGE FUEL</p>
              <h2>87%</h2>
            </div>

            <div className="gauge">
              <span>87%</span>
            </div>
          </div>
        </div>

        {/* 🔥 الخريطة */}
        <div id="map"></div>
      </div>
    </div>
  );
}