var exec = require('cordova/exec');

var AdhanAlarm = {
  schedule: function (alarms, success, error) {
    exec(success, error, 'AdhanAlarm', 'schedule', [alarms || []]);
  },
  cancelAll: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'cancelAll', []);
  },
  requestPermissions: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'requestPermissions', []);
  },
  canScheduleExactAlarms: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'canScheduleExactAlarms', []);
  },
  openExactAlarmSettings: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'openExactAlarmSettings', []);
  },
  openBatterySettings: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'openBatterySettings', []);
  },
  isIgnoringBatteryOptimizations: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'isIgnoringBatteryOptimizations', []);
  },
  stopPlayback: function (success, error) {
    exec(success, error, 'AdhanAlarm', 'stopPlayback', []);
  }
};

module.exports = AdhanAlarm;
