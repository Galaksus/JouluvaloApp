const LightSwitch = document.getElementById('switch-1');
const dimmerSwitch = document.getElementById('switch-2');
const BluetoothDialog = document.getElementById("bluetooth-dialog");
const BluetoothButton = document.getElementById("bluetooth-icon");
const ConnectBluetoothButton = document.getElementById(
  "connect-bluetooth-button"
);
const BluetoothConnectionText = document.getElementById(
  "connection-state-text"
);
var dimmerSlider = document.getElementById("range1");
var delaySlider = document.getElementById("range2");
var dimmerSliderValue = document.getElementById("value1");
var delaySliderValue = document.getElementById("value2");

let BluetoothConnectionState = 10; // default value meaning "Not connected"


// Function to close the dialog
function closeDialog(element) {
    element.style.display = "none";
  }

BluetoothButton.addEventListener("click", function () {
  BluetoothDialog.style.display = "block";
});

ConnectBluetoothButton.addEventListener("click", function () {
  Android.connectBluetooth();
});

const BluetoothConnectionStates = {
  0: "Disconnected",
  1: "Connecting...",
  2: "Connected",
  3: "Disconnecting...",
  10: "Not connected",
  11: "device was not found",
  12: "Scanning for bluetooth devices...",
};

function setBluetoothConnectionStateText(BLEState) {
  // Convert BLEState to a number explicitly
  BluetoothConnectionState = parseInt(BLEState, 10);

  // Set correct text from BluetoothConnectionStates
  if (BluetoothConnectionStates.hasOwnProperty(BluetoothConnectionState)) {
    console.log(BluetoothConnectionStates[BluetoothConnectionState]);
    const text = BluetoothConnectionStates[BluetoothConnectionState];
    BluetoothConnectionText.textContent = text;

    // Use a switch statement to set the text color based on the connection state
    switch (BluetoothConnectionState) {
      case 1:
      case 2:
      case 12:
        BluetoothConnectionText.style.color = "green"; // Connected (green color)
        ConnectBluetoothButton.disabled = true;
        break;
      case 0:
      case 3:
      case 10:
      case 11:
        BluetoothConnectionText.style.color = "red"; // Disconnected, Disconnecting, or Device not found (red color)
        ConnectBluetoothButton.disabled = false;
        break;
      default:
        BluetoothConnectionText.style.color = defaultColor;
        ConnectBluetoothButton.disabled = false;
        break;
    }
  } else {
    BluetoothConnectionText.textContent = "Unknown";
    BluetoothConnectionText.style.color = defaultColor;
  }
}

const BLEConnectedElementIDs = {
    dimmerSlider_: 3, // OK
    delaySlider_: 2,// OK
    commonWidget_: 1,
   // dimmerSwitch_: 1, 
    //LightSwitch_: 0,
  };

  const requiredModes = {
    lights_off: 0,
    lights_on: 1,
    dimmerAuto: 2,
    dimmerManual: 3,
  };



LightSwitch.addEventListener("change", function () {
    // Your logic when the switch is toggled
    console.log("LightSwitch toggled:", LightSwitch.checked);

    var lightsParagraph = document.getElementById("lights-status");

    // Check the status of the checkbox
    if (!LightSwitch.checked) {
        lightsParagraph.textContent = "Lights: OFF";
        Android.JSToBLEInterface(
          BLEConnectedElementIDs.commonWidget_,
          requiredModes.lights_off
        );
        if (dimmerSwitch.checked) {
          dimmerSwitch.click();
        }
    } else {

        // If checkbox is not checked, update the text
        lightsParagraph.textContent = "Lights: ON";
        Android.JSToBLEInterface(
          BLEConnectedElementIDs.commonWidget_,
          requiredModes.lights_on
        );
    }
});

dimmerSwitch.addEventListener("change", function () {
    // Your logic when the switch is toggled
    console.log("dimmerSwitch toggled:", this.checked);
    var dimmerParagraph = document.getElementById("dimmer-status");

    // Check the status of the checkbox
    if (!dimmerSwitch.checked) {
        // If checkbox is checked, update the text
        dimmerParagraph.textContent = "Dimmer: Manual";
        Android.JSToBLEInterface(
          BLEConnectedElementIDs.commonWidget_,
          requiredModes.dimmerManual
        );
    } else {
        // If checkbox is not checked, update the text
        dimmerParagraph.textContent = "Dimmer: Automatic";
        Android.JSToBLEInterface(
          BLEConnectedElementIDs.commonWidget_,
          requiredModes.dimmerAuto
        );
    }
});


// Steering sliders
dimmerSlider.addEventListener("input", function () {
  // Get the original value from the dimmerSlider
  var originalValue = parseInt(dimmerSlider.value);

  // Scale the values
  var scaledValue0to100 = (originalValue / 255) * 100;
  var scaledValue0to5 = (originalValue / 255) * 5;

  // Update the textContent of dimmerSliderValue with the scaled values
  dimmerSliderValue.textContent = `${scaledValue0to100.toFixed(0)} % | ${scaledValue0to5.toFixed(2)} V`;

      Android.JSToBLEInterface(
        BLEConnectedElementIDs.dimmerSlider_,
        parseInt(dimmerSlider.value)
      );
  });
  
  // dimmerSlider
  dimmerSlider.addEventListener("touchend", function () {
    // Timeout is to ensure that bluetooth has enough time to receive the value of the slider at touchend
    setTimeout(function () {
      Android.JSToBLEInterface(
        BLEConnectedElementIDs.dimmerSlider_,
        parseInt(dimmerSlider.value)
      );
    }, 75);
  });
  // Tyhmää toistoa koodille tässä ja vähän ylempänä mutta ei jaksa nyt tehä paremmin
  var originalValue = parseInt(dimmerSlider.value);

  // Scale the values
  var scaledValue0to100 = (originalValue / 255) * 100;
  var scaledValue0to5 = (originalValue / 255) * 5;

  // Update the textContent of dimmerSliderValue with the scaled values
  dimmerSliderValue.textContent = `${scaledValue0to100.toFixed(0)} % | ${scaledValue0to5.toFixed(2)} V`;
  
// Delay slider
delaySlider.addEventListener("input", function () {
  var originalValue = parseInt(delaySlider.value);

  // Scale the values
  var scaledValueSingleCycleToFullCycle = (originalValue*255*2)/1000; // To get seconds of the full cycle 0 to 255 to 0

    delaySliderValue.textContent = `${scaledValueSingleCycleToFullCycle.toFixed(2)} s`;
    Android.JSToBLEInterface(
      BLEConnectedElementIDs.delaySlider_,
      parseInt(delaySlider.value)
    );
  });
  
  delaySlider.addEventListener("touchend", function () {
    // Timeout is to ensure that bluetooth has enough time to receive the value of the slider at touchend
    setTimeout(function () {
      Android.JSToBLEInterface(
        BLEConnectedElementIDs.delaySlider_,
        parseInt(delaySlider.value)
      );
    }, 75);
  });
  delaySliderValue.textContent = `5.1 s`;




// Initialize default value
setBluetoothConnectionStateText(BluetoothConnectionState);