# gpioremotecontrol
Own binding for control GpioControl of a remote Raspberry for openHAB

## Config in *.item file
In the **items-file** of openHAB the following **configuration** is needed:<br>
`Switch|Dimmer|String Name-of-Item { gpioremotecontrol="IPAddressWithHost;PinNumber;Direction" }` for output<br>
`Number Name-of-Item { gpioremotecontrol="IPAddressWithHost;DeviceIdOfSensor;temperature" }` for 1Wire temperature sensor DS18B20<br>

**Examples:**<br>
`Switch lamp1 123.123.123.31:1234;1;out`<br>
`String lamp1 123.123.123.31:1234;1;out` - fading and blinking possible, see in section _StringCommands_ <br>
`Dimmer lamp1 123.123.123.31:1234;1;out`<br>

`Number temperature_out 123.123.123.31:1234;28-00044a7273ff;temperature` - Will get the measured temperature of sensor with ID 28-00044a7273ff<br>

## Config in *.sitemap file
**Example** to control from the website:<br>
`Switch item=lamp1 mappings=[fade_2000_0_100="slowUp", blink_30_200_100_true_10="blink", ON="on", OFF="off"]` - If item defined as String <br>
`Switch item=lamp1` - If item defined as switch <br>
`Slider item=lamp1 sendFrequency=30` - If item defined as Dimmer <br>
`Switch item=lamp1 mappings=[0="Off",50="mid",80="bright",100="Full"]` - If item defined as Dimmer <br>
`Text   item=temperature label="Temperature: [%.1f]"`

## StringCommands
I found no other way to transfer parameter to the binding but seperate a string with underscores.<br> 
Possible are:<br><br>
`dim_VALUE`<br>

`fade_cycleDuration_pwmStartValue_pwmEndValue` - short version<br> 
`fade_cycleDuration_pwmStartValue_pwmEndValue_repeat_cycles_cyclePause`<br>

`fadeUpDown_cycleDuration_pwmStartValue_pwmEndValue` - short version<br>
`fadeUpDown_cycleDuration_pwmStartValue_pwmEndValue_repeat_cycles_cyclePause`<br>

`blink_uptime` - Short version: Only uptime - one flash to 100%.
`blink_uptime_downtime_pwmValue_repeat_cycles` - pwmValue is only relevant for the uptime. Value while downtime is always OFF=0.<br>

All time values in milliseconds. PWM values 0-100. Repeat: true/false. 