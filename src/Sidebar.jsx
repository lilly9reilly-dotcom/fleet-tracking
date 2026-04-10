export default function Sidebar() {
  return (
    <div className="sidebar">
      <h2>Fleet</h2>

      <ul>
        <li className="active">Dashboard</li>
        <li>Alerts</li>
        <li>Geofences</li>
        <li>Routes</li>
        <li>Schedule</li>
      </ul>
    </div>
  );
}