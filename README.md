MQTT Ref:
- https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/?fbclid=IwAR0Uv5VfrScR6UdLh8EAzQLoveLaBAkpuX8LJdIevCZJTHqT4Fp0an0sLvU

FIREBASE:
- https://fir-testing-e07e2.firebaseio.com/
- https://firebase.google.com/docs/database/android/start/

BLE:
- https://stackoverflow.com/questions/9231598/how-to-read-all-bytes-together-via-bluetooth
- https://www.instructables.com/id/Android-Bluetooth-Control-LED-Part-2/
- https://stackoverflow.com/questions/32656510/register-broadcast-receiver-dynamically-does-not-work-bluetoothdevice-action-f
- https://developer.android.com/guide/topics/connectivity/bluetooth#java

DB Types of plant (Humid parameter based on Internet)

	PLANT{
		000{
			id: 000
			name: Cress	//Rau mam
			Humid{
				max: 95
				min: 80
			}
		}
			001{
			id: 001
			name: Succulent	//Sen da
			Humid{
				max: 70
				min: 40
			}
		}
		002{
			id: 002
			name: Catus //Xuong rong
			Humid{
				max: 70
				min: 20
			}
		}		
	}

Database Devices

	DB{
		User0{
			MAC0{
				tree: 
					000
					001
				pot1{
					type: 000
					auto: true
       					commands{
          					key0
	        	  				id: key0
        					      	value: 123
							time: dd/mm/yyy hh:mm:ss
		
       	  					key1
         						id: key1
              						value: 456
							time: dd/mm/yyy hh:mm:ss
				        	}
			        	logs{
		        		  	key2
				              		id: key2
					              	value: 911
							time: dd/mm/yyy hh:mm:ss
						        key3
       					      		id: key3
				             	 	value 905
							time: dd/mm/yyy hh:mm:ss
			        	}
			        	data{
					        key4
			              		id: key4
				              		value: 234
							time: dd/mm/yyy hh:mm:ss
				          	key5
				              		id: key5
				              		value: 345
							time: dd/mm/yyy hh:mm:ss
			        	}
				 }
					pot2{
					type: 001
					auto: false
				}
			   }
 			MAC1{
				pot2{
				}

				pot3{
				}
			}
	}

	User1{
	}

Each BLE have unique MAC address -> represent an area.
1 area have many pot, max 5 pot (1->5), can contains different kind of plants.
Current is 2 pots only.

## RASPBERRY PI

### FB PLANT UPDATE
- zUPy: update PLANT type pot y

### CONTROL CODE
- 1<=y<=5, 00<=xx<=99
- zAT0: auto water all pots
- zATy: auto water pot y only
- z: MAC address (17 chars)
- z93y: Pi -> MQTT
- z94y: MQTT -> Pi
- zxxy: Pi -> Device
- water with xx% humidity, xx is maximum humidity that requires for the plant at pot y
- yxx: Pi -> Device

### DATA
- zyxx: Device -> Pi -> FB
- send data to mqtt and firebase, xx is humidity at pot y

### LOG
- 90y: Device -> Pi
- z90y: Pi -> FB
- Meaning: water successfully

- 91y: Device -> Pi
- z91y: Pi -> FB
- Meaning: water error

- z92: Pi -> FB
- Meaning: BLE MAC error connection

- 92y: Device -> Pi
- z92y: Pi -> FB
- Meaning: Pot y at BLE MAC error connection

MQTT have channels:
- humid: write only
- command: read only
- log: write only
